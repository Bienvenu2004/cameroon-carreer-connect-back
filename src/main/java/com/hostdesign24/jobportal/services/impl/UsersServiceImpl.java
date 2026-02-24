package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.exception.InvalidVerificationException;
import com.hostdesign24.jobportal.exception.TooManyAttemptsException;
import com.hostdesign24.jobportal.mapper.UserMapper;
import com.hostdesign24.jobportal.model.EmailVerification;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import com.hostdesign24.jobportal.repository.EmailVerificationRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.services.EmailService;
import com.hostdesign24.jobportal.services.StorageService;
import com.hostdesign24.jobportal.services.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersServiceImpl implements UsersService {
    private static final int MAX_VERIFICATION_ATTEMPTS = 100;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final EmailVerificationRepository emailVerificationRepository;
    private final StorageService storageService;
    private final EmailService emailService;

    @Value("${app.client-url:}")
    private String clientAppUrl;

    @Override
    public User createUser(@Valid UserRegistrationDto request) {
        log.info("Completing registration for email: {}", request.getEmail());
        EmailVerification verifiedEmail = emailVerificationRepository.findFirstByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(request.getEmail(),
                        VerificationType.EMAIL_REGISTRATION,
                        true)
                .orElseThrow(() -> {
                    log.error("User's email not verified");
                    return new InvalidVerificationException("User's email not verified");
                });

        if (!verifiedEmail.isEmailVerified()) {
            throw new InvalidVerificationException("User's email not verified");
        }

        // Check max attempts
        if (verifiedEmail.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            log.error("Max verification attempts exceeded for: {}", request.getEmail());
            emailVerificationRepository.delete(verifiedEmail);
            throw new TooManyAttemptsException(
                    "Too many failed attempts. Please start registration again.");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        user.setActive(true);
        // Send welcome email
        sendWelcomeEmail(user);
        log.info("User {} created successfully with role {}",
                user.getEmail(), user.getRole());

        if (user.getRole().equals(UserRole.RECRUITER)) {
            recruiterProfileRepository.save(new RecruiterProfile(user));
        } else {
            jobSeekerProfileRepository.save(new JobSeekerProfile(user));
        }

        return user;
    }

    @Override
    public Object getCurrentUserProfile() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            assert authentication != null;
            String username = authentication.getName();
            User user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
            UUID userId = user.getId();

            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
                return recruiterProfileRepository.findById(userId).orElse(new RecruiterProfile(user));
            } else {
                return jobSeekerProfileRepository.findById(userId).orElse(new JobSeekerProfile(user));
            }
        }

        return null;
    }

    @Override
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            assert authentication != null;
            String username = authentication.getName();
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not found user"));
        }

        return null;
    }

    @Override
    public UserDto getCurrentUserDto() {
        User currentUser = getCurrentUser();
        return userMapper.toUserDto(currentUser);
    }

    @Override
    public User findByEmail(String currentUsername) {
        return userRepository.findByEmail(currentUsername).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private void sendWelcomeEmail(User user) {
        try {
            String logo = storageService.getLogoUrl();
            String dashboardUrl = buildDashboardUrl();
            Map<String, Object> templateModel = Map.of(
                    "name", "new user",
                    "email", user.getEmail(),
                    "role", user.getRole().toString(),
                    "year", String.valueOf(Year.now().getValue()),
                    "logoUrl", logo,
                    "loginUrl", dashboardUrl
            );

            emailService.sendEmail(
                    user.getEmail(),
                    "Welcome to Our Platform!",
                    "welcome",
                    templateModel
            );

            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            // Don't fail if welcome email fails
        }
    }

    private String buildDashboardUrl() {
        if (clientAppUrl == null || clientAppUrl.isBlank()) {
            log.warn("app.client-url is not configured. Dashboard link will not be included.");
            return "#";
        }

        return clientAppUrl.endsWith("/")
                ? clientAppUrl + "dashboard"
                : clientAppUrl + "/dashboard";
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }
}