package com.hostdesign24.jobportal.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every message now goes out with a plain-text part beside the HTML, because
 * HTML-only automated mail is what spam filters expect from spammers. That part
 * is derived from the HTML, so it is only worth sending if what the reader
 * needs survives the conversion: the code, and the full address of any link.
 */
class EmailPlainTextTest {

    @Test
    @DisplayName("the plain-text password reset keeps the code and the full link")
    void passwordResetSurvivesConversion() {
        String url = "https://jobs.example.cm/reset-password?email=a%40b.cm&code=123456";
        String text = EmailServiceImpl.toPlainText(render("password-reset", Map.of(
                "name", "a@b.cm",
                "resetCode", "123456",
                "expiryMinutes", 10,
                "year", "2026",
                "resetUrl", url,
                "logo", "cid:" + EmailSender.LOGO_CID)));

        assertTrue(text.contains("123456"), text);
        assertTrue(text.contains(url), "the link must appear unescaped and whole:\n" + text);
        assertFalse(text.contains("<"), "no markup may leak into the text part:\n" + text);
        assertFalse(text.contains("\n\n\n"), "blank lines must be collapsed:\n" + text);
    }

    @Test
    @DisplayName("the logo every template embeds is on the classpath")
    void embeddedLogoExists() {
        assertNotNull(getClass().getClassLoader().getResource("email/logo-white.png"));
    }

    private static String render(String template, Map<String, Object> variables) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariables(variables);
        return engine.process(template, context);
    }
}
