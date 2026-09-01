package com.hostdesign24.jobportal.dto;

import com.hostdesign24.jobportal.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for {@code POST /api/hjp/auth/google}.
 *
 * <p>{@code credential} is the Google ID token (a signed JWT) produced by
 * Google Identity Services in the browser.</p>
 *
 * <p>{@code role} is only consulted when the account does not yet exist — i.e.
 * on first sign-up. It lets the registration screen choose between
 * JOB_SEEKER and RECRUITER. For an already-registered user it is ignored and
 * the stored role wins. When omitted, new accounts default to JOB_SEEKER.</p>
 */
@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank
    private String credential;

    private UserRole role;
}
