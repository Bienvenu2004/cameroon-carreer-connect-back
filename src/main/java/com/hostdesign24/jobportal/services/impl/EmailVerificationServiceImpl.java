package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.RegisterInitDto;
import com.hostdesign24.jobportal.dto.VerifyEmailDto;
import com.hostdesign24.jobportal.exception.*;
import com.hostdesign24.jobportal.model.EmailVerification;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import com.hostdesign24.jobportal.repository.EmailVerificationRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.services.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RateLimitingService rateLimitingService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final GeolocationService geolocationService;

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_VERIFICATION_ATTEMPTS = 10;
    private static final int MAX_INITIATION_REQUESTS_PER_HOUR = 100;
    private static final String USER_NOT_FOUND = "User not found";
    private final StorageService storageService;

    @Value("${app.client-url}")
    private String devFrontUrl;

    @Transactional
    @Override
    public void initiateUserEmailVerification(RegisterInitDto request) {
        String userEmail = request.getEmail();

        if (userRepository.existsByEmailAndDeletedFalse(userEmail)) {
            throw new UserAlreadyExistsException("User already exists");
        }

        if (rateLimitingService.isRegistrationInitiationTooFrequent(
                userEmail, MAX_INITIATION_REQUESTS_PER_HOUR)) {
            throw new RateLimitExceededException(
                    "Too many registration attempts. Please try again later.");
        }

        String verificationCode = generateSecureCode();
        createEmailVerification(userEmail, verificationCode, VerificationType.EMAIL_REGISTRATION);
        sendEmailVerification(userEmail, verificationCode);

        log.info("Registration initiated. Verification code sent.");
    }

    private EmailVerification createEmailVerification(
            String email,
            String code,
            VerificationType type) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or blank");
        }

        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setEmailVerificationCode(passwordEncoder.encode(code));
        verification.setVerificationType(type);
        verification.setEmailVerified(false);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));
        verification.setVerificationAttempts(0);
        verification.setMaxAttempts(MAX_VERIFICATION_ATTEMPTS);

        return emailVerificationRepository.save(verification);
    }

    @Transactional
    @Override
    public void verifyEmail(VerifyEmailDto request) {
        if (request.getVerificationCode() == null) {
            throw new InvalidTokenException("Verification code required");
        }

        if (request.getVerificationType() == null) {
            throw new IllegalArgumentException("Invalid Verification type");
        }

        String email = request.getEmail();
        String verificationCode = request.getVerificationCode();

        EmailVerification verification = getMostRecentNonVerifiedEmailVerification(request, email);

        if (verification.getVerificationAttempts() >= verification.getMaxAttempts()) {
            log.error("Max verification attempts exceeded for verification ID: {}",
                    verification.getId());
            throw new TooManyAttemptsException(
                    "Too many failed attempts. Please request a new verification code.");
        }

        boolean matches = passwordEncoder.matches(
                verificationCode,
                verification.getEmailVerificationCode());

        if (!matches) {
            int attempts = verification.getVerificationAttempts() + 1;
            verification.setVerificationAttempts(attempts);
            emailVerificationRepository.save(verification);

            log.error("Invalid verification code. Attempt {}/{}",
                    attempts, verification.getMaxAttempts());

            int remaining = verification.getMaxAttempts() - attempts;
            if (remaining > 0) {
                throw new InvalidTokenException(
                        String.format("Invalid verification code. %d attempts remaining.", remaining));
            } else {
                throw new TooManyAttemptsException(
                        "Too many failed attempts. Please request a new verification code.");
            }
        }

        verification.setEmailVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setVerificationAttempts(0);
        emailVerificationRepository.save(verification);

        log.info("Email verified successfully for: {}", email);
    }

    private EmailVerification getMostRecentNonVerifiedEmailVerification(VerifyEmailDto request, String email) {
        return emailVerificationRepository
                .findByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(
                        email,
                        request.getVerificationType(),
                        false)
                .stream()
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No valid verification record found");
                    return new InvalidVerificationException(
                            "No valid verification found. Please request a new code.");
                });
    }

    @Transactional
    @Override
    public void resendVerificationEmail(String email, VerificationType type) {
        String newCode = generateSecureCode();
        EmailVerification verification = createEmailVerification(email, newCode, type);

        switch (type) {
            case PASSWORD_RESET -> {
                User user = findUserByEmail(email);
                verification.setUserId(user.getId());
                verification.setResetRequestedAt(LocalDateTime.now());
                emailVerificationRepository.save(verification);
                sendPasswordResetEmail(email, newCode, user);
                log.info("Resent password reset email for: {}", email);
            }
            case EMAIL_REGISTRATION -> {
                sendEmailVerification(email, newCode);
                log.info("Resent registration verification email for: {}", email);
            }
            case NEW_DEVICE_LOGIN -> {
                User user = findUserByEmail(email);
                verification.setUserId(user.getId());
                emailVerificationRepository.save(verification);
                sendEmailVerification(email, newCode);
                log.info("Resent new device login verification email for: {}", email);
            }
            case EMAIL_CHANGE, TWO_FACTOR_AUTH -> {
                User user = findUserByEmail(email);
                verification.setUserId(user.getId());
                emailVerificationRepository.save(verification);
                sendEmailVerification(email, newCode);
                log.info("Resent {} verification email for: {}", type, email);
            }
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    private void sendEmailVerification(String email, String code) {
        try {
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "email", email,
                    "verificationCode", code,
                    "expiryMinutes", VERIFICATION_CODE_EXPIRY_MINUTES,
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    email,
                    "Verify Your Email Address",
                    "email-verification",
                    templateModel
            );

            log.info("Verification email sent");
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }
    }

    private void sendPasswordResetEmail(String email, String code, User user) {
        try {
            String resetUrl = devFrontUrl + "/auth/forgot-password/code?confirmation_code=" + code + "&email=" + email;
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "name", user.getEmail(),
                    "resetCode", code,
                    "year", String.valueOf(Year.now().getValue()),
                    "resetUrl", resetUrl,
                    "logo", logo
            );

            emailService.sendEmail(
                    user.getEmail(),
                    "Password Reset Request",
                    "password-reset",
                    templateModel
            );

            log.info("Password reset email sent");
        } catch (Exception e) {
            log.error("Failed to send password reset verification email", e);
        }
    }

    /**
     * Generate 6-digit verification code (industry standard)
     */
    private String generateSecureCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(1_000_000); // 0-999999
        return String.format("%06d", number);
    }

    @Transactional
    @Override
    public void initiateEmailDeviceVerification(String email, String deviceId, String deviceName, String ipAddress) {

        String verificationCode = generateSecureCode();

        EmailVerification verification = createEmailVerification(
                email,
                verificationCode,
                VerificationType.NEW_DEVICE_LOGIN);

        verification.setDeviceIdentifier(deviceId);
        verification.setMetadata(buildMetadata(deviceName, ipAddress));
        emailVerificationRepository.save(verification);

        // Send email with device details
        sendDeviceVerificationEmail(email, verificationCode, deviceName, ipAddress);

        log.info("Device verification initiated for: {} - Device: {} from IP: {}",
                email, deviceName, ipAddress);
    }

    private String buildMetadata(String deviceName, String ipAddress) {
        return String.format("{\"deviceName\":\"%s\",\"ipAddress\":\"%s\",\"timestamp\":\"%s\"}",
                deviceName, ipAddress, LocalDateTime.now());
    }

    private void sendDeviceVerificationEmail(String email, String code, String deviceName, String ipAddress) {
        try {
            String location = geolocationService.getLocationFromIP(ipAddress);
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "email", email,
                    "verificationCode", code,
                    "deviceName", deviceName,
                    "ipAddress", ipAddress,
                    "location", location,
                    "loginTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    "expiryMinutes", VERIFICATION_CODE_EXPIRY_MINUTES,
                    "securityUrl", "https://yourapp.com/security/report-suspicious-activity",
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    email,
                    "New Device Login Detected",
                    "device-verification",
                    templateModel
            );

            log.info("Device verification email sent to {} for device: {}", email, deviceName);
        } catch (Exception e) {
            log.error("Failed to send device verification email to {} for device: {}", email, deviceName, e);
        }
    }
}