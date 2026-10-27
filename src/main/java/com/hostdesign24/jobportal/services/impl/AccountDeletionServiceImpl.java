package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.DeleteAccountRequest;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobApplication;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.ApplicationStatus;
import com.hostdesign24.jobportal.model.enums.AuthProvider;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.CompanyFollowRepository;
import com.hostdesign24.jobportal.repository.CompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerApplyRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.JobSeekerSaveRepository;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.SavedSearchRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.services.AccountDeletionService;
import com.hostdesign24.jobportal.services.UsersService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/**
 * Self-service account deletion.
 *
 * The account is anonymised rather than removed row by row. Applications, jobs
 * and companies reference it from tables other people still use (a recruiter's
 * pipeline, a company's history), so the rows stay and lose everything that
 * identifies the person: the email is replaced, the password scrambled and the
 * profile emptied. Replacing the email matters most: users.email is unique, so a
 * soft-deleted row that kept it would block the address from ever registering
 * again, which is exactly what someone deleting an account to change role needs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private static final String DELETED_EMAIL_DOMAIN = "@deleted.invalid";

    private final UsersService usersService;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobSeekerApplyRepository applicationRepository;
    private final JobSeekerSaveRepository savedJobRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final CompanyFollowRepository companyFollowRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void deleteCurrentAccount(DeleteAccountRequest request) {
        User current = usersService.getCurrentUser();
        if (current == null || current.getId() == null) {
            throw new EntityNotFoundException("Current user not found");
        }
        // Re-read inside this transaction so the changes below are tracked.
        User user = userRepository.findByIdAndDeletedFalse(current.getId())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        if (user.getRole() == UserRole.SYSTEM_ADMIN) {
            throw new ActionDeniedException("Administrator accounts cannot be deleted from the profile page.");
        }
        checkConfirmation(user, request);

        String formerEmail = user.getEmail();
        if (user.getRole() == UserRole.RECRUITER) {
            eraseRecruiterData(user);
        } else {
            eraseJobSeekerData(user);
        }

        user.setEmail("deleted-" + user.getId() + DELETED_EMAIL_DOMAIN);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setDeleted(true);
        // Tokens carry the password-change time; moving it invalidates every
        // session still open on other devices.
        user.setPasswordChangedAt(new Date());
        userRepository.save(user);

        log.info("Account {} deleted by its owner (was {})", user.getId(), maskEmail(formerEmail));
    }

    private void checkConfirmation(User user, DeleteAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Confirmation is required to delete the account");
        }
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            String typed = request.getConfirmEmail();
            if (typed == null || !typed.trim().equalsIgnoreCase(user.getEmail())) {
                throw new ActionDeniedException("Type your email address exactly to confirm deletion");
            }
            return;
        }
        String password = request.getPassword();
        if (password == null || password.isEmpty() || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ActionDeniedException("Incorrect password");
        }
    }

    private void eraseJobSeekerData(User user) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(user.getId());
        if (profile == null) {
            return;
        }
        UUID profileId = profile.getId();

        // Open applications are withdrawn so recruiters stop waiting on someone
        // who has left; closed ones stay as they were in the recruiter's history.
        for (JobApplication application : applicationRepository.findByProfileIdAndDeletedFalse(profileId)) {
            ApplicationStatus status = application.getStatus();
            if (status == null || !status.isClosed()) {
                application.setStatus(ApplicationStatus.WITHDRAWN);
                application.setStatusReason("The candidate deleted their account");
            }
            application.setCoverLetter(null);
        }

        savedJobRepository.deleteAll(savedJobRepository.findByProfileIdAndDeletedFalse(profileId));
        savedSearchRepository.deleteAll(savedSearchRepository.findByProfileIdAndDeletedFalse(profileId));
        companyFollowRepository.deleteAll(companyFollowRepository.findByProfileIdAndDeletedFalse(profileId));

        profile.setFirstName("Deleted");
        profile.setLastName("account");
        profile.setPhoneNumber(null);
        profile.setAddress(null);
        profile.setWorkAuthorization(null);
        profile.setEmploymentType(null);
        profile.setSpokenLanguages(null);
        profile.setGithubUrl(null);
        profile.setLinkedinUrl(null);
        profile.setWebsiteUrl(null);
        profile.setPortfolioUrl(null);
        profile.setTwitterUrl(null);
        profile.setFacebookUrl(null);
        profile.setResume(null);
        profile.setVideoResume(null);
        profile.setProfilePhoto(null);
        if (profile.getSkills() != null) profile.getSkills().clear();
        if (profile.getExperiences() != null) profile.getExperiences().clear();
        if (profile.getEducations() != null) profile.getEducations().clear();
        profile.setSearchable(false);
        profile.setSearchableSince(null);
        jobSeekerProfileRepository.save(profile);
    }

    private void eraseRecruiterData(User user) {
        // Nobody is left to answer applicants, so the offers come down and the
        // companies this recruiter registered leave the public directory.
        for (Job job : jobRepository.findByCreatedByAndDeletedFalse(user.getId())) {
            job.setActive(false);
        }
        for (Company company : companyRepository.findByCreatedByAndDeletedFalse(user.getId())) {
            company.setDeleted(true);
        }

        RecruiterProfile profile = recruiterProfileRepository.findByUserId(user.getId());
        if (profile != null) {
            profile.setFirstName("Deleted");
            profile.setLastName("account");
            profile.setCity(null);
            profile.setState(null);
            profile.setCountry(null);
            profile.setCompany(null);
            profile.setProfilePhoto(null);
            recruiterProfileRepository.save(profile);
        }
    }

    private static String maskEmail(String email) {
        if (email == null || email.length() < 5 || !email.contains("@")) return "***";
        return email.substring(0, 2) + "***" + email.substring(email.lastIndexOf('@'));
    }
}
