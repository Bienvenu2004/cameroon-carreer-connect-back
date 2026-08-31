package com.hostdesign24.jobportal.security;

import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.services.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Token issuing and validation.
 *
 * These run with a real {@link JwtService} and a real {@link JwtBlacklistService}
 * -- no Spring context and no database -- so they stay fast enough to run on every
 * push. The only collaborator that needs arranging is {@link JwtConfig}, which is a
 * plain properties holder.
 */
class JwtServiceTest {

    /** Long enough to satisfy HS256's 256-bit minimum key length. */
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-signing";

    private static final long FIFTEEN_MINUTES = 15 * 60 * 1000L;
    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000L;
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    private JwtService jwtService;
    private JwtBlacklistService blacklistService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(config(FIFTEEN_MINUTES, SEVEN_DAYS, ONE_DAY),
                blacklistService = new JwtBlacklistService());
        user = user(UserRole.JOB_SEEKER);
    }

    private static JwtConfig config(long access, long refresh, long maxRefreshAge) {
        JwtConfig c = new JwtConfig();
        c.setSecret(SECRET);
        c.setAccessTokenExpiration(access);
        c.setRefreshTokenExpiration(refresh);
        c.setMaxRefreshAge(maxRefreshAge);
        return c;
    }

    private static User user(UserRole role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("seeker@camjobs.cm");
        u.setRole(role);
        return u;
    }

    @Nested
    @DisplayName("access tokens")
    class AccessTokens {

        @Test
        @DisplayName("a freshly issued access token validates and carries the user id")
        void freshTokenIsValid() {
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.validateAccessToken(token)).isTrue();
            assertThat(jwtService.getUserIdFromJwtToken(token))
                    .isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("an expired access token is rejected")
        void expiredTokenIsRejected() {
            // Negative lifetime => the token is already past its expiry when issued.
            JwtService expiring = new JwtService(
                    config(-1000L, SEVEN_DAYS, ONE_DAY), new JwtBlacklistService());

            String token = expiring.generateAccessToken(user);

            assertThat(expiring.validateAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("a token signed with a different secret is rejected")
        void foreignSignatureIsRejected() {
            JwtService attacker = new JwtService(
                    config(FIFTEEN_MINUTES, SEVEN_DAYS, ONE_DAY), new JwtBlacklistService());
            // Same shape, different key.
            JwtConfig otherKey = config(FIFTEEN_MINUTES, SEVEN_DAYS, ONE_DAY);
            otherKey.setSecret("a-completely-different-secret-key-of-sufficient-length-here");
            String forged = new JwtService(otherKey, new JwtBlacklistService())
                    .generateAccessToken(user);

            assertThat(attacker.validateAccessToken(forged)).isFalse();
        }

        @Test
        @DisplayName("a blacklisted token stops validating after logout")
        void blacklistedTokenIsRejected() {
            String token = jwtService.generateAccessToken(user);
            assertThat(jwtService.validateAccessToken(token)).isTrue();

            blacklistService.blacklist(jwtService.getTokenId(token));

            assertThat(jwtService.validateAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("garbage is rejected rather than throwing")
        void malformedTokenIsRejected() {
            assertThat(jwtService.validateAccessToken("not-a-jwt")).isFalse();
        }
    }

    @Nested
    @DisplayName("refresh tokens")
    class RefreshTokens {

        @Test
        @DisplayName("a refresh token validates as a refresh token")
        void refreshTokenIsValid() {
            assertThat(jwtService.validateRefreshToken(jwtService.generateRefreshToken(user)))
                    .isTrue();
        }

        @Test
        @DisplayName("an access token cannot be used to refresh")
        void accessTokenIsNotARefreshToken() {
            String access = jwtService.generateAccessToken(user);

            assertThat(jwtService.validateRefreshToken(access)).isFalse();
        }

        @Test
        @DisplayName("a refresh token older than maxRefreshAge is rejected even before it expires")
        void refreshTokenPastMaxAgeIsRejected() {
            // Lifetime is still seven days, but the sliding re-use window is zero,
            // so the token is too old the instant it is issued.
            JwtService shortWindow = new JwtService(
                    config(FIFTEEN_MINUTES, SEVEN_DAYS, 0L), new JwtBlacklistService());

            String token = shortWindow.generateRefreshToken(user);

            assertThat(shortWindow.validateRefreshToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("password-change invalidation")
    class PasswordChange {

        @Test
        @DisplayName("a token issued before a password change is invalid afterwards")
        void tokenIssuedBeforePasswordChangeIsInvalid() {
            user.setPasswordChangedAt(new Date(1_000_000L));
            String token = jwtService.generateAccessToken(user);

            // The user changes their password: the stored timestamp moves.
            user.setPasswordChangedAt(new Date(2_000_000L));

            assertThat(jwtService.userTokenIsInvalid(token, user)).isTrue();
        }

        @Test
        @DisplayName("a token issued after the last password change stays valid")
        void tokenMatchingPasswordChangeIsValid() {
            user.setPasswordChangedAt(new Date(1_000_000L));
            String token = jwtService.generateAccessToken(user);

            assertThat(jwtService.userTokenIsInvalid(token, user)).isFalse();
        }

        @Test
        @DisplayName("a legacy token without the claim is rejected once the password has changed")
        void legacyTokenRejectedAfterPasswordChange() {
            // No passwordChangedAt at issue time => no pwd_changed claim.
            String legacy = jwtService.generateAccessToken(user);

            user.setPasswordChangedAt(new Date(2_000_000L));

            assertThat(jwtService.userTokenIsInvalid(legacy, user)).isTrue();
        }
    }
}
