package com.hostdesign24.jobportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for which browser origins may talk to this backend.
 *
 * Previously the REST CORS mapping (WebConfig) and the STOMP handshake
 * (WebSocketConfig) each decided this for themselves, and they disagreed:
 * REST was restricted to the configured client URL while the socket accepted
 * {@code "*"}. With cookie-based authentication that wildcard undid the
 * restriction taken on the REST side, so both now resolve their origins here.
 *
 * The Vite dev server is included so local development works without extra
 * configuration; it is harmless in production because a deployed frontend
 * never reports {@code localhost:5173} as its origin.
 */
@Component
public class AllowedOrigins {

    private static final List<String> DEV_ORIGINS =
            List.of("http://localhost:5173", "http://127.0.0.1:5173");

    private final List<String> origins;

    public AllowedOrigins(@Value("${app.client-url:}") String clientUrl) {
        List<String> resolved = new ArrayList<>(DEV_ORIGINS);
        if (clientUrl != null && !clientUrl.isBlank()) {
            resolved.add(clientUrl.trim());
        }
        this.origins = List.copyOf(resolved);
    }

    /** Origins allowed to make credentialed browser requests, as an array. */
    public String[] asArray() {
        return origins.toArray(String[]::new);
    }

    public List<String> asList() {
        return origins;
    }
}
