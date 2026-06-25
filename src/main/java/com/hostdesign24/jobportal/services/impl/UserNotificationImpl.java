package com.hostdesign24.jobportal.services.impl;


import com.hostdesign24.jobportal.dto.NotificationRequestDto;
import com.hostdesign24.jobportal.exception.NotificationProcessingException;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.services.NotificationFactoryService;
import com.hostdesign24.jobportal.services.NotificationPublisherService;
import com.hostdesign24.jobportal.services.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationImpl implements UserNotificationService {
    private final NotificationFactoryService factoryService;
    private final NotificationPublisherService publisherService;

    @Override
    public void createAccountNotification(User user) {
        UUID userId = validateAndGetUserId(user);
        sendNotification(userId, "CreateAccount", () ->
                factoryService.createAccountCreationNotification(userId));
    }

    @Override
    public void passwordResetNotification(User user) {
        UUID userId = validateAndGetUserId(user);
        sendNotification(userId, "PasswordReset", () ->
                factoryService.createPasswordResetNotification(userId));
    }

    @Override
    public void newConnectionDeviceNotification(User user, String deviceName, String deviceIp) {
        UUID userId = validateAndGetUserId(user);
        sendNotification(userId, "AlertConnection", () ->
                factoryService.userNewConnection(userId, deviceName, deviceIp));
    }

    @Override
    public void newJobApplicationNotification(UUID recruiterId, String candidateName, String jobTitle, UUID applicationId) {
        Assert.notNull(recruiterId, "Recruiter ID cannot be null");
        sendNotification(recruiterId, "NewApplication", () ->
                factoryService.newJobApplication(recruiterId, candidateName, jobTitle, applicationId));
    }

    @Override
    public void applicationStatusChangedNotification(UUID jobSeekerId, String jobTitle, String newStatus, UUID applicationId) {
        Assert.notNull(jobSeekerId, "Job seeker ID cannot be null");
        sendNotification(jobSeekerId, "ApplicationStatusChanged", () ->
                factoryService.applicationStatusChanged(jobSeekerId, jobTitle, newStatus, applicationId));
    }

    /**
     * Template method for sending notifications with consistent error handling and logging.
     * Modifié pour accepter `userId` (sécurise les logs et évite double validation).
     */
    private void sendNotification(UUID userId, String notificationType,
                                  ThrowingSupplier<NotificationRequestDto> notificationSupplier) {
        try {
            NotificationRequestDto notification = notificationSupplier.get();
            publisherService.persistAndPublish(notification);
            log.info("Successfully sent {} notification for userId={}", notificationType, userId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for {} notification for userId={}: {}", notificationType, userId, e.getMessage());
            throw e;
        } catch (NotificationProcessingException e) {
            log.error("NotificationProcessingException while sending {} for userId={}: {}",
                    notificationType, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to send {} notification for userId={}", notificationType, userId);
            log.debug("Detailed error while sending {} notification for userId={}", notificationType, userId, e);
            throw new NotificationProcessingException(
                    "Failed to process " + notificationType + " notification", e);
        }
    }

    private UUID validateAndGetUserId(User user) {
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(user.getId(), "User ID cannot be null");
        return user.getId();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws NotificationProcessingException;
    }

}
