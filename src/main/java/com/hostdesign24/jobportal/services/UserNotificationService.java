package com.hostdesign24.jobportal.services;


import com.hostdesign24.jobportal.model.User;

public interface UserNotificationService {
    void createAccountNotification(User user);
    void passwordResetNotification(User user);
    void newConnectionDeviceNotification(User user, String deviceName, String deviceIp);
}
