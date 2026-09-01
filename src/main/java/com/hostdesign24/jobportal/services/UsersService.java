package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.UserDto;
import com.hostdesign24.jobportal.dto.UserRegistrationDto;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.security.GoogleUserInfo;
import jakarta.validation.Valid;

import java.util.Optional;

public interface UsersService {
    User createUser(@Valid UserRegistrationDto dto);

    /**
     * Resolve the local account backing a verified Google identity, creating
     * it on first sign-in. Existing accounts (matched by email) are returned
     * as-is — {@code desiredRole} is only used when a brand-new account is
     * provisioned. New accounts are active, email-verified (Google vouched for
     * it) and get a matching seeker/recruiter profile pre-filled with the
     * Google name.
     *
     * @param info        the trusted claims from the verified Google ID token
     * @param desiredRole the role chosen on the sign-up screen, or {@code null}
     *                    to default to {@link UserRole#JOB_SEEKER}
     * @return the persisted (existing or newly created) user
     */
    User findOrCreateGoogleUser(GoogleUserInfo info, UserRole desiredRole);

    Object getCurrentUserProfile();

    User getCurrentUser();

    UserDto getCurrentUserDto();

    User findByEmail(String currentUsername);

    Optional<User> getUserByEmail(String email);

    boolean emailExists(String email);
}
