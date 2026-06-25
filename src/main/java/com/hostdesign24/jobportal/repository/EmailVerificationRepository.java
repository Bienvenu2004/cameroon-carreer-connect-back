package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.EmailVerification;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findFirstByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(String email,
                                                                                                        VerificationType verificationType,
                                                                                                        boolean emailVerified);
    List<EmailVerification> findByEmailAndVerificationTypeAndEmailVerifiedOrderByCreatedAtDesc(
            String email,
            VerificationType verificationType,
            boolean emailVerified);

    long countByEmailAndVerificationTypeAndCreatedAtAfter(
            String email,
            VerificationType verificationType,
            LocalDateTime after);

    // For cleanup jobs - remove old unverified records
    List<EmailVerification> findByEmailVerifiedFalseAndExpiresAtBefore(LocalDateTime dateTime);

    Optional<EmailVerification> findByEmailAndDeviceIdentifierAndVerificationTypeAndEmailVerifiedFalse(String email, String deviceId, VerificationType verificationType);
}
