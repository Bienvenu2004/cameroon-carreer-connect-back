package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.TokenResolver;
import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.*;
import com.hostdesign24.jobportal.dto.userDevice.VerifyDeviceDto;
import com.hostdesign24.jobportal.exception.*;
import com.hostdesign24.jobportal.mapper.UserMapper;
import com.hostdesign24.jobportal.model.EmailVerification;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.UserDevice;
import com.hostdesign24.jobportal.model.enums.DeviceStatus;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import com.hostdesign24.jobportal.repository.EmailVerificationRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.security.JwtConfig;
import com.hostdesign24.jobportal.security.JwtService;
import com.hostdesign24.jobportal.services.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.SecurityException;
import java.util.Locale;
import java.time.Year;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Auth service implementation for registering and authenticating users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final Integer MAX_RESET_ATTEMPTS = 20;
    private static final String USER_NOT_FOUND = "User not found";

    /**
     * Whether session cookies should carry the Secure flag.
     * MUST be true in production (HTTPS only). Defaults to false so dev
     * over plain http://localhost works out of the box; flip this to true
     * via the env var APP_COOKIES_SECURE=true when deploying.
     */
    @Value("${app.cookies.secure:false}")
    private boolean cookieSecure;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;
    private final EmailService emailService;
    private final UserMapper mapper;
    private final LoginAttemptService loginAttemptService;
    private final JwtBlacklistService blacklistService;
    private final RateLimitingService rateLimitingService;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserDeviceService userDeviceService;
    private final NotificationAsyncService notificationAsyncService;
    private final UserNotificationService userNotificationService;

    @Override
    public AuthenticationResponse authenticateUser(
            AuthenticationRequest authenticationRequest,
            HttpServletResponse response,
            String clientTypeHeader,
            HttpServletRequest httpRequest) {

        final String ip = Utils.getClientIp();

        checkRateLimits(ip);
        authenticateCredentials(authenticationRequest, ip);
        markLoginAsSucceeded(ip);

        User user = getUserByEmail(authenticationRequest.getEmail());
        User userId = getUserById(user.getId());

        String userAgent = httpRequest.getHeader("User-Agent");
        String deviceId = userDeviceService.generateDeviceId(userAgent, ip);
        String deviceName = userDeviceService.extractDeviceName(userAgent);

        try {
            //checkDeviceStatus(user, deviceId, deviceName, ip);

            log.info("User {} authenticated successfully from verified device: {} (IP: {})",
                    user.getEmail(), deviceName, ip);

            userDeviceService.recordLoginActivity(user, deviceId, ip, true, null);

            userNotificationService.newConnectionDeviceNotification(userId, deviceName, deviceId);
            notificationAsyncService.notifyDeviceLogin(user.getEmail(), deviceName, ip);

            updateUserLogin(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            UserDto userDto = mapper.toUserDto(user);
            userDto.setRole(user.getRole());

            return getResponse(response, clientTypeHeader, accessToken, refreshToken, userDto);
        } catch (Exception e) {
            try {
                userDeviceService.recordLoginActivity(user, deviceId, ip, false, e.getMessage());
            } catch (Exception recEx) {
                log.error("Failed to record failed login activity for user {} device {}: {}", user.getEmail(), deviceId, recEx.getMessage(), recEx);
            }
            throw e;
        }
    }

    private AuthenticationResponse getResponse(HttpServletResponse response, String clientTypeHeader, String accessToken, String refreshToken, UserDto userDto) {
        if ("web".equalsIgnoreCase(clientTypeHeader)) {
            setTokenCookies(response, accessToken, refreshToken);
            return AuthenticationResponse.builder()
                    .user(userDto)
                    .message("Login successful")
                    .build();
        } else {
            return AuthenticationResponse.builder()
                    .user(userDto)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .message("Login successful")
                    .build();
        }
    }

    private void updateUserLogin(User user) {
        user.setLastLogin(new Date());
        user.setLastTokenRefresh(new Date());
        userRepository.save(user);
    }

    private void markLoginAsSucceeded(String ip) {
        loginAttemptService.loginSucceeded(ip);
    }

    private void checkRateLimits(String ip) {
        if (loginAttemptService.isBlocked(ip)) {
            throw new TooManyRequestsException(
                    "You have exceeded the maximum number of failed login attempts. Please try again in 15 minutes.");
        }
    }

    private void authenticateCredentials(AuthenticationRequest authenticationRequest, String ip) {
        String normalizedEmail = normalizeEmail(authenticationRequest.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            authenticationRequest.getPassword()));
        } catch (AuthenticationException ex) {
            loginAttemptService.loginFailed(ip); // Track failed attempt
            log.warn(
                    "Authentication failed for {} with {}: {}",
                    normalizedEmail,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw new BadCredentialsException("Incorrect email or password provided");
        }
    }

    private void checkDeviceStatus(User user, String deviceId, String deviceName, String ip) {
        Optional<UserDevice> existingDevice = userDeviceService.getDevice(user.getId(), deviceId);
        if (existingDevice.isPresent()) {
            UserDevice device = existingDevice.get();
            DeviceStatus status = device.getDeviceStatus();

            if (status == DeviceStatus.BLOCKED) {
                log.warn("Blocked device attempted login for user {}: {} from IP: {}",
                        user.getEmail(), deviceName, ip);
                throw new DeviceBlockedException(
                        "This device has been blocked. Please contact support or use a different device.");
            }

            if (status == DeviceStatus.INACTIVE) {
                log.warn("Inactive device attempted login for user {}: {} from IP: {}",
                        user.getEmail(), deviceName, ip);
                throw new DeviceBlockedException(
                        "This device has been deactivated. Please verify your device again or contact support.");
            }
        }
    }


    @Override
    public String changePassword(PasswordUpdateDto passwordUpdateDto) {
        User currentUser = Utils.getCurrentUser()
                .orElseThrow(() -> new SecurityException("User not logged in"));
        var userId = currentUser.getId();
        log.info("User {} is changing password", userId);
        var user = getUserById(userId);
        var newPassword = passwordUpdateDto.getNewPassword();
        var confirmPassword = passwordUpdateDto.getConfirmPassword();
        var currentPassword = passwordUpdateDto.getCurrentPassword();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.error("Current password is incorrect");
            throw new PasswordNotMatchException("Current password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            log.error("New password and confirm password is incorrect");
            throw new PasswordNotMatchException("New password and confirm password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.error("New password must be different from current password");
            throw new PasswordNotMatchException("New password must be different from current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(new Date());
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", user.getEmail());
        return "Password changed successfully";
    }

    @Override
    public AuthenticationResponse refreshToken(String refreshToken, HttpServletResponse response,
                                               String clientTypeHeader) {

        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new InvalidJwtException("Invalid or expired refresh token");
        }
        var userId = jwtService.getUserIdFromJwtToken(refreshToken);
        var user = getUserByIdString(userId);

        if (jwtService.userTokenIsInvalid(refreshToken, user)) {
            throw new InvalidJwtException("Invalid JWT token for user");
        }
        user.setLastTokenRefresh(new Date());
        userRepository.save(user);
        var accessToken = jwtService.generateAccessToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);

        if ("web".equalsIgnoreCase(clientTypeHeader)) {
            setTokenCookies(response, accessToken, newRefreshToken);
            return AuthenticationResponse.builder()
                    .build();
        } else {
            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        }
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Password reset process initiated for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmailAndDeletedFalse(email);

        if (userOpt.isEmpty()) {
            log.warn("Password reset attempted for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        if (rateLimitingService.isPasswordResetTooFrequent(user.getId(), MAX_RESET_ATTEMPTS)) {
            log.warn("Too many reset requests for user: {}", user.getEmail());
            return;
        }

        emailVerificationService.resendVerificationEmail(email, VerificationType.PASSWORD_RESET);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void completePasswordReset(ResetPasswordRequest request) {
        log.info("Attempting to complete password reset");

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        EmailVerification emailVerification = emailVerificationRepository.findFirstByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(
                        request.getEmail(),
                        VerificationType.PASSWORD_RESET,
                        true)
                .orElseThrow(
                        () -> new InvalidTokenException("Invalid or expired password reset code.")
                );

        if (emailVerification.getResetRequestedAt() == null) {
            throw new InvalidTokenException("Not a valid password reset request");
        }

        if (emailVerification.getVerifiedAt()
                .isBefore(emailVerification.getResetRequestedAt())) {
            throw new InvalidTokenException("Invalid or expired password reset code");
        }

        User user = getUserByEmail(request.getEmail());

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(new Date());
        userRepository.save(user);

        Map<String, Object> templateModel = Map.of(
                "name", user.getEmail(),
                "date", user.getPasswordChangedAt(),
                "year", String.valueOf(Year.now().getValue()),
                "companyName", "RETMS"
        );

        // Send confirmation email
        emailService.sendEmail(
                user.getEmail(),
                "Password Changed Successfully",
                "password-change-confirmation",
                templateModel
        );

        log.info("Password reset completed for user {}", user.getEmail());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        TokenResolver.resolveFromHttpRequest(request).ifPresent(token -> {
            String jti = jwtService.getTokenId(token);
            blacklistService.blacklist(jti);
        });

        invalidateCookies(response);
        SecurityContextHolder.clearContext();
    }


    private void setTokenCookies(HttpServletResponse response, String accessToken,
                                 String refreshToken) {
        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(cookieSecure);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (jwtConfig.getAccessTokenExpiration() / 1000));

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(cookieSecure);
        // Path was "/api/v1/auth" — wrong, the controller is at /api/hjp/auth
        // and we also need this cookie on /api/hjp/auth/logout. Use "/" so the
        // browser sends it on every refresh / logout call.
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (jwtConfig.getRefreshTokenExpiration() / 1000));

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    private void invalidateCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("access_token", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(cookieSecure);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);

        Cookie refreshTokenCookie = new Cookie("refresh_token", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(cookieSecure);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    @Transactional
    @Override
    public AuthenticationResponse verifyDeviceAndAuthenticate(
            VerifyDeviceDto request,
            HttpServletResponse response,
            String clientTypeHeader) {

        // Verify the code
        VerifyEmailDto verifyEmailDto = new VerifyEmailDto();
        verifyEmailDto.setEmail(request.getEmail());
        verifyEmailDto.setVerificationCode(request.getVerificationCode());
        verifyEmailDto.setVerificationType(VerificationType.NEW_DEVICE_LOGIN);

        emailVerificationService.verifyEmail(verifyEmailDto);

        // Get user
        User user = getUserByEmail(request.getEmail());

        // Verify and activate the device
        userDeviceService.verifyDevice(user.getId(), request.getDeviceId());

        // Generate tokens
        updateUserLogin(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        UserDto userDto = mapper.toUserDto(user);

        if ("web".equalsIgnoreCase(clientTypeHeader)) {
            setTokenCookies(response, accessToken, refreshToken);
            return AuthenticationResponse.builder()
                    .user(userDto)
                    .requiresDeviceVerification(false)
                    .build();
        } else {
            return AuthenticationResponse.builder()
                    .user(userDto)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .requiresDeviceVerification(false)
                    .build();
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private User getUserById(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    private User getUserByIdString(String id) {
        return userRepository.findByIdAndDeletedFalse(UUID.fromString(id))
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    @Override
    public AuthenticationResponse requestDeviceVerification(AuthenticationRequest authenticationRequest, HttpServletResponse response, String clientType, HttpServletRequest httpRequest) {

        final String ip = Utils.getClientIp();
        authenticateCredentials(authenticationRequest, ip);
        markLoginAsSucceeded(ip);

        User user = getUserByEmail(authenticationRequest.getEmail());
        String userAgent = httpRequest.getHeader("User-Agent");
        String deviceId = userDeviceService.generateDeviceId(userAgent, ip);
        String deviceName = userDeviceService.extractDeviceName(userAgent);
        checkDeviceStatus(user, deviceId, deviceName, ip);

        return userDeviceService.initiateDeviceVerification(user, deviceId, deviceName, ip, userAgent);
    }
}