package com.hostdesign24.jobportal.services.impl;

import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated  // Added for input validation
public class EmailSender {

    private static final int MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024; // 10MB limit
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Content-ID of the logo carried inside every message. Templates point at
     * "cid:" + LOGO_CID instead of a URL: a remote image that fails to load (a
     * localhost address, a sleeping host) is a classic spam signal, and many
     * clients block remote images anyway.
     */
    public static final String LOGO_CID = "jcc-logo";
    private static final String LOGO_RESOURCE = "email/logo-white.png";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    // Display name only. This used to read spring.mail.from, which held an email
    // address, so recipients saw an address where the sender's name belongs.
    @Value("${spring.mail.from-name:JobsConnect CMR}")
    private String fromName;

    /**
     * Sends an email asynchronously with retry capability
     */
    @Async
    @Retryable(
            retryFor = {MailException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 5000)
    )
    public void sendEmailAsync(
            @NotBlank @Email String to,
            @NotBlank String subject,
            @NotBlank String htmlBody,
            @NotBlank String textBody) {
        sendEmail(to, subject, htmlBody, textBody, null, null);
    }

    /**
     * Sends an email with attachment asynchronously with retry capability
     */
    @Async
    @Retryable(
            retryFor = {MailException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 5000)
    )
    public void sendEmailWithAttachment(
            @NotBlank @Email String to,
            @NotBlank String subject,
            @NotBlank String htmlBody,
            @NotNull byte[] attachment,
            @NotBlank String filename) {
        sendEmail(to, subject, htmlBody, null, attachment, filename);
    }

    /**
     * Core email sending logic used by both public methods
     */
    private void sendEmail(String to, String subject, String htmlBody, String textBody,
                           byte[] attachment, String filename) {
        // Log masked email address for security
        String maskedEmail = maskEmail(to);
        log.info("Attempting to send email to {}...", maskedEmail);

        try {
            validateAttachment(attachment);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromAddress, fromName);
            helper.setReplyTo(fromAddress);
            helper.setSubject(subject);
            // Spam filters score HTML-only mail as suspicious; a real plain-text
            // alternative is what legitimate senders ship.
            if (textBody != null) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(htmlBody, true);
            }
            // RFC 3834: marks the message as automated, so auto-responders stay quiet.
            mimeMessage.setHeader("Auto-Submitted", "auto-generated");

            // Inline parts must be added after the body.
            if (htmlBody.contains("cid:" + LOGO_CID)) {
                helper.addInline(LOGO_CID, new ClassPathResource(LOGO_RESOURCE), "image/png");
            }

            if (attachment != null && filename != null) {
                helper.addAttachment(filename, new ByteArrayResource(attachment));
            }

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", maskedEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", maskedEmail, e);
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    private void validateAttachment(byte[] attachment) {
        if (attachment != null && attachment.length > MAX_ATTACHMENT_SIZE) {
            throw new EmailSendingException("Attachment size exceeds maximum allowed size of " + MAX_ATTACHMENT_SIZE + " bytes");
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 5) return "***";
        return email.substring(0, 2) + "***" + email.substring(email.lastIndexOf("@"));
    }

    // Custom exception class
    public static class EmailSendingException extends MailException {
        public EmailSendingException(String message) {
            super(message);
        }

        public EmailSendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}