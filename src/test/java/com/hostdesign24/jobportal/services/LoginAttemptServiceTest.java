package com.hostdesign24.jobportal.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Brute-force lockout on the login endpoint.
 *
 * This is the control that actually protects sign-in -- the Bucket4j interceptor
 * had been registered on a path prefix no controller used, so until that was fixed
 * this cache was the only thing standing between an attacker and unlimited password
 * guesses. Worth pinning down.
 */
class LoginAttemptServiceTest {

    private static final String IP = "41.202.0.17";
    private static final int MAX_ATTEMPTS = 5;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    @DisplayName("an unseen client is not blocked")
    void unknownClientIsAllowed() {
        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    @DisplayName("four failures are tolerated, the fifth locks the client out")
    void locksOutOnTheFifthFailure() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            service.loginFailed(IP);
            assertThat(service.isBlocked(IP))
                    .as("blocked after %d failure(s)", i + 1)
                    .isFalse();
        }

        service.loginFailed(IP);

        assertThat(service.isBlocked(IP)).isTrue();
    }

    @Test
    @DisplayName("a successful login clears the failure count")
    void successResetsTheCounter() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            service.loginFailed(IP);
        }

        service.loginSucceeded(IP);

        // The counter is back to zero, so the next failure is the first, not the fifth.
        service.loginFailed(IP);
        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    @DisplayName("lockout is per client, not global")
    void lockoutIsScopedToOneClient() {
        String otherIp = "197.155.64.9";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            service.loginFailed(IP);
        }

        assertThat(service.isBlocked(IP)).isTrue();
        assertThat(service.isBlocked(otherIp))
                .as("a second user behind a different address must not be locked out")
                .isFalse();
    }

    @Test
    @DisplayName("attempts past the threshold keep the client blocked")
    void staysBlockedBeyondTheThreshold() {
        for (int i = 0; i < MAX_ATTEMPTS + 10; i++) {
            service.loginFailed(IP);
        }

        assertThat(service.isBlocked(IP)).isTrue();
    }
}
