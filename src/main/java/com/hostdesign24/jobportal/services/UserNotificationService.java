package com.hostdesign24.jobportal.services;


import com.hostdesign24.jobportal.model.User;

import java.util.UUID;

public interface UserNotificationService {
    void createAccountNotification(User user);
    void passwordResetNotification(User user);
    void newConnectionDeviceNotification(User user, String deviceName);
    void newJobApplicationNotification(UUID recruiterId, String candidateName, String jobTitle, UUID applicationId);
    void applicationStatusChangedNotification(UUID jobSeekerId, String jobTitle, String newStatus, UUID applicationId);
    void jobInvitationNotification(UUID jobSeekerId, String jobTitle, String companyName, UUID jobId);
    void jobReportResolvedNotification(UUID reporterId, String jobTitle, boolean upheld, UUID jobId);
    void newJobAtFollowedCompanyNotification(UUID jobSeekerId, String companyName, String jobTitle, UUID jobId);
}
