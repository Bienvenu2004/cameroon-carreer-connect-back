package com.hostdesign24.jobportal.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hostdesign24.jobportal.exception.InvalidJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.oauth.google.client-id:}") String clientId) {
        if (clientId != null && !clientId.isBlank()) {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
            log.info("Google Sign-In enabled (audience configured).");
        } else {
            this.verifier = null;
            log.warn("Google Sign-In disabled: app.oauth.google.client-id (GOOGLE_CLIENT_ID) is not set.");
        }
    }

    /** Whether a client id was configured. */
    public boolean isEnabled() {
        return verifier != null;
    }

    /**
     * Validate the given Google credential and return its trusted claims.
     *
     * @throws IllegalStateException if Google Sign-In is not configured
     * @throws InvalidJwtException   if the token is missing, invalid, expired,
     *                               for the wrong audience, or the email is
     *                               not verified by Google
     */
    public GoogleUserInfo verify(String credential) {
        if (verifier == null) {
            throw new IllegalStateException("Google Sign-In is not configured on this server.");
        }

        final GoogleIdToken idToken;
        try {
            idToken = verifier.verify(credential);
        } catch (Exception e) {
            log.error("Google ID token verification threw", e);
            throw new InvalidJwtException("Could not verify Google credential.");
        }

        if (idToken == null) {
            throw new InvalidJwtException("Invalid Google credential.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified == null || !emailVerified) {
            throw new InvalidJwtException("Your Google account email is not verified.");
        }

        return new GoogleUserInfo(
                payload.getEmail(),
                (String) payload.get("given_name"),
                (String) payload.get("family_name"),
                (String) payload.get("name"),
                (String) payload.get("picture"),
                payload.getSubject()
        );
    }
}
