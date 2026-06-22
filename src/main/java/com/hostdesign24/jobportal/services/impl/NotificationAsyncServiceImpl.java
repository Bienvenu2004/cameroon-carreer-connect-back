package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.EmailService;
import com.hostdesign24.jobportal.services.GeolocationService;
import com.hostdesign24.jobportal.services.NotificationAsyncService;
import com.hostdesign24.jobportal.services.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Async
    @Override
    public void notifyNewApplication(String recruiterEmail, String candidateName, String jobTitle, String candidateEmail) {
        try {
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "candidateName", candidateName,
                    "candidateEmail", candidateEmail,
                    "jobTitle", jobTitle,
                    "applicationDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    "dashboardUrl", "https://yourapp.com/dashboard/applications",
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    recruiterEmail,
                    "New Application: " + candidateName + " for " + jobTitle,
                    "new-application",
                    templateModel
            );

            log.info("New application notification sent to recruiter {} for job: {}", recruiterEmail, jobTitle);
        } catch (Exception e) {
            log.error("Failed to send new application email to {}", recruiterEmail, e);
        }
    }

    @Async
    @Override
    public void notifyApplicationHired(String seekerEmail, String seekerName, String jobTitle, String companyName) {
        try {
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "name", seekerName,
                    "jobTitle", jobTitle,
                    "companyName", companyName,
                    "dashboardUrl", "https://yourapp.com/dashboard/applications",
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    seekerEmail,
                    "Congratulations! You've been hired for " + jobTitle,
                    "application-hired",
                    templateModel
            );

            log.info("Hired notification sent to {} for job: {}", seekerEmail, jobTitle);
        } catch (Exception e) {
            log.error("Failed to send hired notification to {}", seekerEmail, e);
        }
    }

    @Async
    @Override
    public void notifyApplicationRejected(String seekerEmail, String seekerName, String jobTitle, String companyName) {
        try {
            String logo = storageService.getLogoUrl();

            Map<String, Object> templateModel = Map.of(
                    "name", seekerName,
                    "jobTitle", jobTitle,
                    "companyName", companyName,
                    "dashboardUrl", "https://yourapp.com/dashboard/jobs",
                    "year", String.valueOf(Year.now().getValue()),
                    "logo", logo
            );

            emailService.sendEmail(
                    seekerEmail,
                    "Update on your application for " + jobTitle,
                    "application-rejected",
                    templateModel
            );

            log.info("Rejection notification sent to {} for job: {}", seekerEmail, jobTitle);
        } catch (Exception e) {
            log.error("Failed to send rejection notification to {}", seekerEmail, e);
        }
    }
}