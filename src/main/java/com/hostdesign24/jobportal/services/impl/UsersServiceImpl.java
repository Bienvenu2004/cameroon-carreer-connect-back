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
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Locale;
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
        String normalizedEmail = normalizeEmail(request.getEmail());
        request.setEmail(normalizedEmail);

        log.info("Completing registration for email: {}", normalizedEmail);
        EmailVerification verifiedEmail = emailVerificationRepository.findFirstByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(normalizedEmail,
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
            log.error("Max verification attempts exceeded for: {}", normalizedEmail);
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
            RecruiterProfile profile = new RecruiterProfile(user);
            profile = recruiterProfileRepository.save(profile);
            user.setRecruiterProfile(profile);
        } else {
            JobSeekerProfile profile = new JobSeekerProfile(user);
            profile = jobSeekerProfileRepository.save(profile);
            user.setJobSeekerProfile(profile);
        }

        return user;
    }

    /**
     * Same rationale as {@link #getCurrentUserDto()} — Jackson will serialize
     * the returned profile entity in the controller, accessing LAZY
     * associations (skills, resume, profile photo) along the way. We need
     * an open Hibernate session for that.
     */
    @Override
    @Transactional(readOnly = true)
    public Object getCurrentUserProfile() {

        // Delegate principal resolution to getCurrentUser() — it knows how
        // to pull the User entity from the JWT-installed principal without
        // the email-lookup pitfall.
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        UUID userId = user.getId();

        if (user.getRole() == UserRole.RECRUITER) {
            return recruiterProfileRepository.findById(userId).orElse(new RecruiterProfile(user));
        } else {
            return jobSeekerProfileRepository.findById(userId).orElse(new JobSeekerProfile(user));
        }
    }

    @Override
    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // JwtFilters installs the raw User entity as the principal on every
        // JWT-authenticated request. That instance was loaded in the filter
        // (with no transaction) and is therefore DETACHED — accessing its
        // LAZY associations (jobSeekerProfile, recruiterProfile) downstream
        // throws LazyInitializationException. We pull just the ID and
        // re-load through the repository so the returned entity is attached
        // to the current request's session.
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            UUID userId = ((User) principal).getId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("Could not find user"));
        }

        // Fallback: during the login flow Spring Security uses a UserDetails
        // principal whose name IS the email — keep looking up by email then.
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Could not find user"));
    }

    /**
     * Loads the current user AND maps it to a UserDto inside a single
     * read-only transaction. The mapping recurses into
     * jobSeekerProfile → skills (a separate LAZY @OneToMany), so we MUST
     * keep the Hibernate session open across the load + map step or
     * `LazyInitializationException` will fire when MapStruct calls
     * `.size()` on the PersistentBag.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
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
        return userRepository.existsByEmailAndDeletedFalse(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}