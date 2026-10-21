package com.hostdesign24.jobportal.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mail template is named by a string, and a string that names nothing
 * compiles perfectly well. The password reset failed in exactly that way: the
 * code asked for "password-reset", no such file existed, and the omission
 * surfaced only as a stack trace when a user tried to recover their account.
 *
 * These tests close that gap by reading the names out of the source itself,
 * so a template added, renamed or deleted is checked without anyone
 * remembering to update a list here.
 */
class EmailTemplateTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    /** The template name is the third argument of sendEmail, after two human strings. */
    private static final Pattern CALL = Pattern.compile("sendEmail\\s*\\(", Pattern.MULTILINE);
    private static final Pattern NAME = Pattern.compile("\"([a-z][a-z0-9-]+)\"");

    @Test
    @DisplayName("every template the code asks for exists")
    void everyRequestedTemplateExists() throws IOException {
        Set<String> requested = requestedTemplateNames();

        // Guard against the scan quietly matching nothing and the test passing
        // for the wrong reason.
        assertTrue(requested.size() >= 10,
                "expected the scan to find the mail templates, found: " + requested);

        Set<String> missing = new TreeSet<>();
        for (String name : requested) {
            if (!Files.exists(TEMPLATES.resolve(name + ".html"))) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(),
                "the code sends these templates but no file defines them: " + missing
                        + " (looked in " + TEMPLATES + ")");
    }

    @Test
    @DisplayName("the password reset email carries the code and a link to the reset page")
    void passwordResetRenders() {
        String url = "http://localhost:5173/reset-password?email=a%40b.cm&code=123456";
        String html = render("password-reset", Map.of(
                "name", "a@b.cm",
                "resetCode", "123456",
                "expiryMinutes", 10,
                "year", "2026",
                "resetUrl", url,
                "logo", "http://localhost:8080/storage/logo.png"));

        assertTrue(html.contains("123456"), "the reset code must appear in the message");
        // The ampersand between the parameters is escaped as &amp; in HTML, so
        // the link is checked by its parts rather than as one literal string.
        assertTrue(html.contains("/reset-password"), "the message must link to the reset page");
        assertTrue(html.contains("email=a%40b.cm"), "the link must carry the address");
        assertTrue(html.contains("code=123456"), "the link must carry the code");
        assertFalse(html.isBlank(), "the template rendered nothing");
    }

    @Test
    @DisplayName("the password change confirmation states when the change happened")
    void passwordChangeConfirmationRenders() {
        String html = render("password-change-confirmation", Map.of(
                "name", "a@b.cm",
                "date", new Date(),
                "year", "2026",
                "companyName", "JobsConnect CMR"));

        assertTrue(html.contains("JobsConnect CMR"), "the platform must be named");
        assertTrue(html.contains("Changed on"), "the message must say when the password changed");
    }

    /* ---------------- helpers ---------------- */

    /** Template names passed to EmailService.sendEmail anywhere in the sources. */
    private Set<String> requestedTemplateNames() throws IOException {
        Set<String> names = new TreeSet<>();

        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher call = CALL.matcher(source);

                while (call.find()) {
                    // The subject and the recipient are ordinary sentences or
                    // variables; a template name is a single lower-case token,
                    // which is what separates it from them.
                    String window = source.substring(
                            call.end(), Math.min(source.length(), call.end() + 400));
                    Matcher name = NAME.matcher(window);
                    if (name.find()) {
                        names.add(name.group(1));
                    }
                }
            }
        }
        return names;
    }

    private String render(String template, Map<String, Object> model) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        // The application's engine, not a plain one: it evaluates expressions
        // with SpEL exactly as the running service does.
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariables(model);
        return engine.process(template, context);
    }
}
