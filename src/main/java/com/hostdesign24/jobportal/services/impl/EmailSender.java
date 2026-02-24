package com.hostdesign24.jobportal.services.impl;

import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${spring.mail.from:LandLordCloud}")
    private String fromName;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private int mailTimeout;

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
            @NotBlank String htmlBody) {
        sendEmail(to, subject, htmlBody, null, null);
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
        sendEmail(to, subject, htmlBody, attachment, filename);
    }

    /**
     * Core email sending logic used by both public methods
     */
    private void sendEmail(String to, String subject, String htmlBody, byte[] attachment, String filename) {
        // Log masked email address for security
        String maskedEmail = maskEmail(to);
        log.info("Attempting to send email to {}...", maskedEmail);

        try {
            validateAttachment(attachment);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromAddress, fromName);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

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