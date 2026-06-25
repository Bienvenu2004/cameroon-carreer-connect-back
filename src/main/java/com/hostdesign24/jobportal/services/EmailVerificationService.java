package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.RegisterInitDto;
import com.hostdesign24.jobportal.dto.VerifyEmailDto;
import com.hostdesign24.jobportal.model.enums.VerificationType;
import jakarta.transaction.Transactional;

public interface EmailVerificationService {
    @Transactional
    void initiateUserEmailVerification(RegisterInitDto userEmail);

    @Transactional
    void verifyEmail(VerifyEmailDto request);

    @Transactional
    void resendVerificationEmail(String email, VerificationType type);

    @Transactional
    void initiateEmailDeviceVerification(String email, String deviceId, String deviceName, String ipAddress);
}
