package com.hostdesign24.jobportal.services;

import org.springframework.scheduling.annotation.Async;

public interface NotificationAsyncService {

    @Async
    void notifyDeviceLogin(String email, String deviceName, String ipAddress);
}
