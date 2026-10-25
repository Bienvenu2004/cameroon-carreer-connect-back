package com.hostdesign24.jobportal.services.impl;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds a real message through EmailSender, without a mail server, and checks
 * the parts that decide whether it lands in the inbox: a plain-text alternative
 * next to the HTML, the logo carried inside the message rather than fetched from
 * a URL, and the sender's display name.
 */
class EmailSenderStructureTest {

    @Test
    @DisplayName("a message carries text and HTML alternatives and the embedded logo")
    void messageStructure() throws Exception {
        CapturingSender mail = new CapturingSender();
        EmailSender sender = new EmailSender(mail);
        ReflectionTestUtils.setField(sender, "fromAddress", "jobs@example.cm");
        ReflectionTestUtils.setField(sender, "fromName", "JobsConnect CMR");

        String html = "<html><body><img src=\"cid:" + EmailSender.LOGO_CID + "\"/><p>Code 123456</p></body></html>";
        sender.sendEmailAsync("seeker@example.cm", "Verify your email", html, "Code 123456");

        MimeMessage message = mail.sent;
        assertNotNull(message, "nothing was sent");
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        String source = raw.toString(StandardCharsets.UTF_8);

        assertTrue(source.contains("multipart/alternative"), "no text/HTML alternative:\n" + source);
        assertTrue(source.contains("text/plain"), "no plain-text part");
        assertTrue(source.contains("text/html"), "no HTML part");
        assertTrue(source.contains("Content-ID: <" + EmailSender.LOGO_CID + ">"), "logo not embedded");
        assertTrue(source.contains("Auto-Submitted: auto-generated"));
        assertEquals("JobsConnect CMR <jobs@example.cm>", message.getHeader("From", null));
    }

    /** Keeps the message instead of opening an SMTP connection. */
    private static final class CapturingSender extends JavaMailSenderImpl {
        MimeMessage sent;

        @Override
        public void send(MimeMessage mimeMessage) {
            try {
                mimeMessage.saveChanges();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            sent = mimeMessage;
        }
    }
}
