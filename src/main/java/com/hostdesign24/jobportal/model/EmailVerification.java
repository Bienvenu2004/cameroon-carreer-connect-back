package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.VerificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "email_verifications")
public class EmailVerification extends BaseEntity {
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 255)
    private String emailVerificationCode; // hashed

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(nullable = false, name = "email_verification_code_expiry")
    private LocalDateTime expiresAt;

    @Column(name = "email_verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "email_verification_attempts")
    private Integer verificationAttempts = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 10;

    // Optional: track device/IP for security
    private String deviceIdentifier;
    private String ipAddress;

    // Optional: reference to user if exists
    private UUID userId;

    private LocalDateTime resetRequestedAt;

    private String metadata;
}
