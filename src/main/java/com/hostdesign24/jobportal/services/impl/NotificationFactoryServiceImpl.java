package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.NotificationRequestDto;
import com.hostdesign24.jobportal.services.NotificationFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class NotificationFactoryServiceImpl implements NotificationFactoryService {

    @Override
    public NotificationRequestDto createAccountCreationNotification(UUID userId) {
        return NotificationRequestDto.builder()
                .recipientId(userId)
                .message("Your account has been successfully created.")
                .type("INFO")
                .relatedEntityType("USER")
                .relatedEntityId(userId)
                .build();
    }

    @Override
    public NotificationRequestDto createPasswordResetNotification(UUID userId) {
        return NotificationRequestDto.builder()
                .recipientId(userId)
                .message("Your password has been successfully reset.")
                .type("INFO")
                .relatedEntityType("USER")
                .relatedEntityId(userId)
                .build();
    }

    @Override
    public NotificationRequestDto userNewConnection(UUID userId, String device, String ip) {
        String message = String.format("New sign-in: %s , %s", device, ip);

        return NotificationRequestDto.builder()
                .recipientId(userId)
                .message(message)
                .type("ALERT")
                .relatedEntityType("USER")
                .relatedEntityId(userId)
                .build();
    }

    @Override
    public NotificationRequestDto newJobApplication(UUID recruiterId, String candidateName, String jobTitle, UUID applicationId) {
        String message = String.format("New application from %s for \"%s\"", candidateName, jobTitle);

        return NotificationRequestDto.builder()
                .recipientId(recruiterId)
                .message(message)
                .type("INFO")
                .relatedEntityType("APPLICATION")
                .relatedEntityId(applicationId)
                .build();
    }

    @Override
    public NotificationRequestDto applicationStatusChanged(UUID jobSeekerId, String jobTitle, String newStatus, UUID applicationId) {
        String message = switch (newStatus) {
            case "REVIEWED" -> String.format("Your application for \"%s\" has been reviewed", jobTitle);
            case "INTERVIEW" -> String.format("You've been invited to interview for \"%s\"", jobTitle);
            case "HIRED" -> String.format("Congratulations! You've been hired for \"%s\"", jobTitle);
            case "REJECTED" -> String.format("Your application for \"%s\" was not selected", jobTitle);
            default -> String.format("Your application for \"%s\" has been updated", jobTitle);
        };

        String type = "HIRED".equals(newStatus) || "REJECTED".equals(newStatus) ? "ALERT" : "INFO";

        return NotificationRequestDto.builder()
                .recipientId(jobSeekerId)
                .message(message)
                .type(type)
                .relatedEntityType("APPLICATION")
                .relatedEntityId(applicationId)
                .build();
    }
}
