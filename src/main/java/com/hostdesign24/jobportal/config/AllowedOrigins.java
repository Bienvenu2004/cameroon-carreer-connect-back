package com.hostdesign24.jobportal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <h2>Why the value is normalised and logged</h2>
 *
 * An origin is matched as a whole string, so a deployment where {@code CLIENT_URL}
 * differs from the browser's origin by one character -- a trailing slash copied
 * from the address bar, quotes a dashboard kept around the value, a misspelt
 * host -- fails every request with a bare {@code 403 Invalid CORS request}. That
 * response names neither the origin it rejected nor the origins it would accept,
 * and the configured value is visible nowhere from outside, so the fault reads
 * as "the backend is broken" and is diagnosed by guesswork. The two shapes a
 * dashboard reliably introduces are therefore repaired here, and the resolved
 * list is logged once at startup so the deployment log answers the question
 * directly.
 *
 * A comma-separated list is accepted so a frontend can be renamed, or a staging
 * host added, without a window where neither URL works.
 */
@Component
public class AllowedOrigins {

    private static final Logger log = LoggerFactory.getLogger(AllowedOrigins.class);

    private static final List<String> DEV_ORIGINS =
            List.of("http://localhost:5173", "http://127.0.0.1:5173");

    private final List<String> origins;

    public AllowedOrigins(@Value("${app.client-url:}") String clientUrl) {
        List<String> resolved = new ArrayList<>(DEV_ORIGINS);

        if (clientUrl != null) {
            for (String candidate : clientUrl.split(",")) {
                String origin = normalise(candidate);
                if (!origin.isBlank() && !resolved.contains(origin)) {
                    resolved.add(origin);
                }
            }
        }

        this.origins = List.copyOf(resolved);

        List<String> configured = origins.stream().filter(o -> !DEV_ORIGINS.contains(o)).toList();
        if (configured.isEmpty()) {
            log.warn("app.client-url resolved to nothing usable (raw value: '{}'). "
                    + "CORS will accept the local dev server only, so every request from a "
                    + "deployed frontend is answered 403 Invalid CORS request.", clientUrl);
        } else {
            log.info("CORS and the STOMP handshake accept these origins: {}", origins);
        }
    }

    /**
     * Reduces a configured URL to the origin a browser would actually send.
     *
     * {@code Origin} is scheme, host and port -- never a path, never a trailing
     * slash -- but {@code CLIENT_URL} is copied from an address bar, so it arrives
     * as {@code https://host/}, {@code https://host/login}, or quoted, depending on
     * what was in the clipboard. Each of those matches no origin at all and fails
     * every request with an unexplained 403, while reading as a perfectly correct
     * value in the dashboard. The path is dropped here rather than rejected,
     * because it is the browser's definition of an origin that governs, not the
     * operator's typing.
     *
     * Only this class discards the path. The same property also builds the links in
     * outgoing email, and a deployment served under a subpath needs it kept there.
     *
     * A misspelt host cannot be repaired -- the startup log exists for that one.
     */
    private static String normalise(String raw) {
        String value = raw.trim();

        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1).trim();
        }

        try {
            java.net.URI uri = java.net.URI.create(value);
            if (uri.getScheme() != null && uri.getHost() != null) {
                return uri.getPort() == -1
                        ? uri.getScheme() + "://" + uri.getHost()
                        : uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            }
        } catch (IllegalArgumentException ignored) {
            // Not parseable as a URI; fall through and salvage what we can below.
        }

        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    /** Origins allowed to make credentialed browser requests, as an array. */
    public String[] asArray() {
        return origins.toArray(String[]::new);
    }

    public List<String> asList() {
        return origins;
    }
}
