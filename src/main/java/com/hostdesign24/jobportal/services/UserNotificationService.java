package com.hostdesign24.jobportal.services;


import com.hostdesign24.jobportal.model.User;

import java.util.UUID;

public interface UserNotificationService {
    void createAccountNotification(User user);
    void passwordResetNotification(User user);
    void newConnectionDeviceNotification(User user, String deviceName, String deviceIp);
    void newJobApplicationNotification(UUID recruiterId, String candidateName, String jobTitle, UUID applicationId);
    void applicationStatusChangedNotification(UUID jobSeekerId, String jobTitle, String newStatus, UUID applicationId);
}
