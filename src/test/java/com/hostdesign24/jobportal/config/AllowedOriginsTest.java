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
    @DisplayName("a trailing slash copied from the address bar still matches the browser's origin")
    void stripsTrailingSlash() {
        // A browser never sends a trailing slash in Origin, so "https://host/"
        // configured here would match nothing and answer every request 403.
        assertThat(new AllowedOrigins("https://jobconnect.cm/").asList())
                .contains("https://jobconnect.cm")
                .doesNotContain("https://jobconnect.cm/");

        assertThat(new AllowedOrigins("https://jobconnect.cm///").asList())
                .contains("https://jobconnect.cm");
    }

    @Test
    @DisplayName("a page path pasted from the address bar is not part of the origin")
    void stripsPath() {
        // The value that broke the Cloudflare deployment: the whole login page URL
        // was pasted into CLIENT_URL. A browser sends Origin without a path, so the
        // configured value matched nothing and every request was answered 403.
        assertThat(new AllowedOrigins("https://jobconnect.cm/login").asList())
                .contains("https://jobconnect.cm")
                .doesNotContain("https://jobconnect.cm/login");

        assertThat(new AllowedOrigins("https://jobconnect.cm/jobs?region=Centre").asList())
                .contains("https://jobconnect.cm");
    }

    @Test
    @DisplayName("an explicit port is kept, since it is part of the origin")
    void keepsPort() {
        assertThat(new AllowedOrigins("http://localhost:4173/login").asList())
                .contains("http://localhost:4173");
    }

    @Test
    @DisplayName("quotes a dashboard kept around the value are not part of the origin")
    void stripsWrappingQuotes() {
        assertThat(new AllowedOrigins("\"https://jobconnect.cm\"").asList())
                .contains("https://jobconnect.cm");

        assertThat(new AllowedOrigins("'https://jobconnect.cm/'").asList())
                .contains("https://jobconnect.cm");
    }

    @Test
    @DisplayName("several origins can be configured at once, so a host can be renamed safely")
    void acceptsCommaSeparatedList() {
        AllowedOrigins origins =
                new AllowedOrigins(" https://old.jobconnect.cm/ , https://new.jobconnect.cm ");

        assertThat(origins.asList())
                .contains("https://old.jobconnect.cm", "https://new.jobconnect.cm", "http://localhost:5173");
    }

    @Test
    @DisplayName("an origin is never listed twice, whatever the configuration repeats")
    void doesNotDuplicate() {
        AllowedOrigins origins =
                new AllowedOrigins("https://jobconnect.cm,https://jobconnect.cm/,http://localhost:5173");

        assertThat(origins.asList()).containsOnlyOnce("https://jobconnect.cm");
        assertThat(origins.asList()).containsOnlyOnce("http://localhost:5173");
    }

    @Test
    @DisplayName("an empty entry in the list is skipped rather than allowing a blank origin")
    void skipsEmptyEntries() {
        assertThat(new AllowedOrigins("https://jobconnect.cm, ,,").asList())
                .contains("https://jobconnect.cm")
                .doesNotContain("");
    }

    @Test
    @DisplayName("asArray mirrors asList, since CORS and STOMP take different shapes")
    void arrayMirrorsList() {
        AllowedOrigins origins = new AllowedOrigins("https://jobconnect.cm");

        assertThat(origins.asArray()).containsExactlyElementsOf(origins.asList());
    }
}
