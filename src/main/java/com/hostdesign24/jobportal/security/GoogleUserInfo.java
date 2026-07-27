package com.hostdesign24.jobportal.security;

/**
 * The trusted subset of claims extracted from a verified Google ID token.
 * Only produced by {@link GoogleTokenVerifier} after the token's signature,
 * audience and expiry have been validated against Google.
 */
public record GoogleUserInfo(
        String email,
        String givenName,
        String familyName,
        String fullName,
        String pictureUrl,
        String subject
) {
}
