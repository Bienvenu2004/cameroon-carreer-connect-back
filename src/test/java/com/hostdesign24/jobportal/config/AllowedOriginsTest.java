package com.hostdesign24.jobportal.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single origin list shared by REST CORS and the STOMP handshake.
 *
 * The socket used to accept {@code "*"} while REST was restricted; these tests pin
 * the resolution rules so the two cannot drift apart again.
 */
class AllowedOriginsTest {

    @Test
    @DisplayName("the configured client URL is allowed alongside the dev server")
    void includesConfiguredClientUrl() {
        AllowedOrigins origins = new AllowedOrigins("https://jobconnect.cm");

        assertThat(origins.asList())
                .contains("https://jobconnect.cm", "http://localhost:5173");
    }

    @Test
    @DisplayName("a blank client URL still yields a usable dev-only list, never a wildcard")
    void blankClientUrlIsIgnored() {
        assertThat(new AllowedOrigins("").asList())
                .containsExactlyInAnyOrder("http://localhost:5173", "http://127.0.0.1:5173")
                .doesNotContain("*");

        assertThat(new AllowedOrigins(null).asList()).doesNotContain("*");
    }

    @Test
    @DisplayName("surrounding whitespace in the configured URL is trimmed")
    void trimsConfiguredClientUrl() {
        assertThat(new AllowedOrigins("  https://jobconnect.cm  ").asList())
                .contains("https://jobconnect.cm");
    }

    @Test
    @DisplayName("an unrelated origin is never allowed")
    void rejectsUnrelatedOrigin() {
        assertThat(new AllowedOrigins("https://jobconnect.cm").asList())
                .doesNotContain("https://attacker.example");
    }

    @Test
    @DisplayName("asArray mirrors asList, since CORS and STOMP take different shapes")
    void arrayMirrorsList() {
        AllowedOrigins origins = new AllowedOrigins("https://jobconnect.cm");

        assertThat(origins.asArray()).containsExactlyElementsOf(origins.asList());
    }
}
