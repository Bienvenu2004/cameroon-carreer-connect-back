package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.DataConflictException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.UserMapper;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.EmailVerificationRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import com.hostdesign24.jobportal.security.GoogleUserInfo;
import com.hostdesign24.jobportal.services.EmailService;
import com.hostdesign24.jobportal.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The role chosen on the sign-up page must decide what Google sign-in does. It
 * used to be ignored for an existing address (a job seeker who chose
 * "Recruiter" was quietly signed in as a job seeker), the sign-in page created
 * job seeker accounts for unknown addresses, and SYSTEM_ADMIN was accepted from
 * the request body.
 */
class GoogleSignInRoleTest {

    private static final String EMAIL = "person@example.cm";
    private static final GoogleUserInfo GOOGLE =
            new GoogleUserInfo(EMAIL, "Ada", "Ngono", "Ada Ngono", null, "google-sub");

    private UserRepository users;
    private RecruiterProfileRepository recruiters;
    private UsersServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        recruiters = mock(RecruiterProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("{noop}x");
        service = new UsersServiceImpl(users, mock(JobSeekerProfileRepository.class), recruiters, encoder,
                mock(UserMapper.class), mock(EmailVerificationRepository.class), mock(StorageService.class),
                mock(EmailService.class));
    }

    private User existing(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setRole(role);
        when(users.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("sign-up as recruiter with an address already registered as job seeker is refused")
    void conflictingRoleIsRefused() {
        existing(UserRole.JOB_SEEKER);
        DataConflictException e = assertThrows(DataConflictException.class,
                () -> service.findOrCreateGoogleUser(GOOGLE, UserRole.RECRUITER));
        assertTrue(e.getMessage().contains("job seeker"), e.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("sign-up with the account's own role signs it in")
    void sameRoleSignsIn() {
        User user = existing(UserRole.RECRUITER);
        assertSame(user, service.findOrCreateGoogleUser(GOOGLE, UserRole.RECRUITER));
    }

    @Test
    @DisplayName("the sign-in page (no role) signs an existing account in with its role")
    void signInWithoutRole() {
        User user = existing(UserRole.JOB_SEEKER);
        assertSame(user, service.findOrCreateGoogleUser(GOOGLE, null));
    }

    @Test
    @DisplayName("the sign-in page does not create an account for an unknown address")
    void unknownAddressWithoutRoleIsNotCreated() {
        when(users.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findOrCreateGoogleUser(GOOGLE, null));
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("sign-up creates the account with the selected role")
    void newAccountTakesSelectedRole() {
        when(users.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recruiters.save(any(RecruiterProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = service.findOrCreateGoogleUser(GOOGLE, UserRole.RECRUITER);

        assertEquals(UserRole.RECRUITER, created.getRole());
        verify(recruiters).save(any(RecruiterProfile.class));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN can never be chosen through Google sign-up")
    void adminRoleIsRejected() {
        assertThrows(ActionDeniedException.class, () -> service.findOrCreateGoogleUser(GOOGLE, UserRole.SYSTEM_ADMIN));
        verify(users, never()).save(any());
    }
}
