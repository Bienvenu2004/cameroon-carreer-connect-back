package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.EmailService;
import com.hostdesign24.jobportal.services.GeolocationService;
import com.hostdesign24.jobportal.services.NotificationAsyncService;
import com.hostdesign24.jobportal.services.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationAsyncServiceImpl implements NotificationAsyncService {

    private final EmailService emailService;
    private final GeolocationService geolocationService;
    private final StorageService storageService;

    @Async
    @Override
    public void notifyDeviceLogin(String email, String deviceName, String ipAddress) {
        try {
            String location = geolocationService.getLocationFromIP(ipAddress);
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "email", email,
                    "deviceName", deviceName,
                    "ipAddress", ipAddress,
                    "location", location,
                    "loginTime", LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    "securityUrl", "https://yourapp.com/security/devices",
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    email,
                    "New Login to Your Account",
                    "device-login-notification",
                    templateModel
            );

            log.info("Login notification email sent to {} for device: {}", email, deviceName);
        } catch (Exception e) {
            log.error("Failed to send login notification email to {}", email, e);
            throw e;
        }
    }
}