package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.*;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.CreationResponse;
import com.hostdesign24.jobportal.dto.userDevice.VerifyDeviceDto;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import com.hostdesign24.jobportal.services.AuthService;
import com.hostdesign24.jobportal.services.EmailVerificationService;
import com.hostdesign24.jobportal.services.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/hjp/auth", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Authentication", description = "Operations for registration, login, token refresh, password management and logout")
public class AuthenticationController {

    private final AuthService authService;
    private final UsersService userService;

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/request-email-verification")
    @Operation(
            summary = "Initiate user registration",
            description = "Initiate the registration of a new user account by verifying user's email."
    )
    public ResponseEntity<ApiResponse<Void>> initiateUserRegistration(
            @RequestBody @Valid RegisterInitDto request) {

        emailVerificationService.initiateUserEmailVerification(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null, "Registration initiated for user with email: " + request.getEmail()));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Create a new user account. Returns the created user's id and a confirmation message."
    )
    public ResponseEntity<ApiResponse<CreationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationRequestDto) {
        User user = userService.createUser(registrationRequestDto);
        CreationResponse data = new CreationResponse(user.getId(), "User created successfully.");

        return new ResponseEntity<>(ApiResponse.success(data, "Resource created"), HttpStatus.CREATED);
    }

    @PostMapping("/resend-verification/{email}")
    @Operation(
            summary = "Resend registration verification email",
            description = "Resend the verification email for a user who has initiated registration."
    )
    public ResponseEntity<ApiResponse<Void>> resendRegistrationVerification(
            @PathVariable String email,
            @RequestParam(defaultValue = "EMAIL_REGISTRATION") VerificationType type) {
        emailVerificationService.resendVerificationEmail(email, type);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null, "Verification email resent successfully"));
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify email",
            description = "Verify user's email using the provided verification code."
    )
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody VerifyEmailDto request) {
        emailVerificationService.verifyEmail(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null, "Email successfully verified"));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticate using credentials and return authentication tokens. May require device verification."
    )
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @RequestBody @Valid AuthenticationRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, required = false, defaultValue = "mobile") String clientType,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        AuthenticationResponse data = authService.authenticateUser(
                request, response, clientType, httpRequest);

        if (data.isRequiresDeviceVerification()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(data, "Device verification required. Check your email."));
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Authentication successful."));
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh authentication token",
            description = "Refresh access token using a refresh token provided either as a cookie or in the request body. Returns new authentication tokens."
    )
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @RequestHeader(value = CLIENT_TYPE_HEADER, required = false, defaultValue = "mobile") String clientType,
            HttpServletResponse response) {

        String token;
        if (cookieToken != null && !cookieToken.isBlank()) {
            token = cookieToken.trim();
        } else if (requestBody != null && requestBody.getRefreshToken() != null) {
            token = requestBody.getRefreshToken().trim();
        } else {
            token = null;
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token is required."));
        }

        AuthenticationResponse data = authService.refreshToken(token, response, clientType);

        return ResponseEntity.ok(ApiResponse.success(data, "Token refreshed successfully."));
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change password",
            description = "Change the authenticated user's password. Requires valid authentication."
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordUpdateDto passwordUpdateDto) {
        authService.changePassword(passwordUpdateDto);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Initiate password reset",
            description = "Start a password reset flow by sending reset instructions to the provided email address."
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "If the account exists, password reset instructions have been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Complete password reset",
            description = "Complete the password reset using a token and new password. Updates the user's password on success."
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest resetPasswordRequest) {
        authService.completePasswordReset(resetPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully."));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Invalidate refresh tokens and perform logout cleanup for the current user."
    )
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful."));
    }

    @GetMapping("/validate-email/{email}")
    @Operation(
            summary = "Validate email existence",
            description = "Check if an email address is already registered in the system."
    )
    @PermitAll
    public ResponseEntity<ApiResponse<Boolean>> validateEmail(@PathVariable String email) {
        boolean emailExists = userService.emailExists(email);
        String message = emailExists
                ? "Email is already registered. Please use a different email address."
                : "Email is available for registration.";

        return ResponseEntity.ok(ApiResponse.success(emailExists, message));
    }

    @PostMapping("/verify-device")
    @Operation(summary = "Verify new device", description = "Verify a new device login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> verifyDevice(
            @RequestBody @Valid VerifyDeviceDto request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, required = false, defaultValue = "mobile") String clientType,
            HttpServletResponse response) {

        AuthenticationResponse data = authService.verifyDeviceAndAuthenticate(request, response, clientType);
        return ResponseEntity.ok(ApiResponse.success(data, "Device verified successfully."));
    }

    @PostMapping("/request-device-verification")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticate using credentials and return authentication tokens. May require device verification."
    )
    public ResponseEntity<ApiResponse<AuthenticationResponse>> requestDeviceVerification(
            @RequestBody @Valid AuthenticationRequest request,
            @RequestHeader(value = CLIENT_TYPE_HEADER, required = false, defaultValue = "mobile") String clientType,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        AuthenticationResponse data = authService.requestDeviceVerification(
                request, response, clientType, httpRequest);

        if (data.isRequiresDeviceVerification()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(data, "Device verification required. Check your email."));
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Authentication successful."));
    }

}