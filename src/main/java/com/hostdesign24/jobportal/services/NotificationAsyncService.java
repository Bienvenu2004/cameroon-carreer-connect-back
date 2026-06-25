package com.hostdesign24.jobportal.services;

import org.springframework.scheduling.annotation.Async;

public interface NotificationAsyncService {

    @Async
    void notifyDeviceLogin(String email, String deviceName, String ipAddress);

    @Async
    void notifyNewApplication(String recruiterEmail, String candidateName, String jobTitle, String candidateEmail);

    @Async
    void notifyApplicationHired(String seekerEmail, String seekerName, String jobTitle, String companyName);

    @Async
    void notifyApplicationRejected(String seekerEmail, String seekerName, String jobTitle, String companyName);
}
