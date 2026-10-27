package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Production had no administrator because the SQL seed skipped itself on a fresh
 * database. The bootstrap must create one from the environment, promote the
 * address if it already exists with another role, and never touch an admin that
 * is already in place (its password may have been changed since).
 */
class AdminAccountBootstrapTest {

    private static final String EMAIL = "admin@example.cm";
    private static final String PASSWORD = "a-long-enough-secret";

    private UserRepository users;
    private AdminAccountBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(inv -> "{enc}" + inv.getArgument(0));
        bootstrap = new AdminAccountBootstrap(users, encoder);
        configure(" Admin@Example.cm ", PASSWORD);
    }

    private void configure(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", email);
        ReflectionTestUtils.setField(bootstrap, "adminPassword", password);
    }

    private User saved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("creates the administrator when the address is unknown")
    void createsAdmin() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        bootstrap.run(null);
        User admin = saved();
        assertEquals(EMAIL, admin.getEmail());
        assertEquals(UserRole.SYSTEM_ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertEquals("{enc}" + PASSWORD, admin.getPassword());
    }

    @Test
    @DisplayName("promotes an existing non-admin account with that address")
    void promotesExisting() {
        User seeker = new User();
        seeker.setEmail(EMAIL);
        seeker.setRole(UserRole.JOB_SEEKER);
        seeker.setDeleted(true);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(seeker));
        bootstrap.run(null);
        User admin = saved();
        assertEquals(UserRole.SYSTEM_ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertFalse(admin.isDeleted());
    }

    @Test
    @DisplayName("leaves an active administrator and its password alone")
    void leavesExistingAdmin() {
        User admin = new User();
        admin.setEmail(EMAIL);
        admin.setRole(UserRole.SYSTEM_ADMIN);
        admin.setActive(true);
        admin.setPassword("{enc}changed-by-owner");
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        bootstrap.run(null);
        verify(users, never()).save(any());
        assertEquals("{enc}changed-by-owner", admin.getPassword());
    }

    @Test
    @DisplayName("does nothing without variables, or with a short password")
    void needsBothVariables() {
        configure("", "");
        bootstrap.run(null);
        configure(EMAIL, "short");
        bootstrap.run(null);
        verify(users, never()).save(any());
    }
}
