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
 * <p>{@code role} is sent by the sign-up screen only (JOB_SEEKER or RECRUITER).
 * With a role, a new account is created with it, and an existing account with a
 * different role is refused with 409 rather than signed in under the old role.
 * Without a role (the sign-in screen), only existing accounts can sign in; an
 * unknown address is told to sign up and choose a role.</p>
 */
@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank
    private String credential;

    private UserRole role;
}
