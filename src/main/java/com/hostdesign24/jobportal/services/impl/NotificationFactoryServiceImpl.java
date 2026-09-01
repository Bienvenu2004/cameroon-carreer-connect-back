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
    public NotificationRequestDto userNewConnection(UUID userId, String device) {
        String message = String.format("New sign-in from %s", device);

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
    public NotificationRequestDto jobInvitation(UUID jobSeekerId, String jobTitle, String companyName, UUID jobId) {
        String message = String.format("%s invited you to apply for \"%s\"", companyName, jobTitle);

        return NotificationRequestDto.builder()
                .recipientId(jobSeekerId)
                .message(message)
                .type("ALERT")
                .relatedEntityType("JOB")
                .relatedEntityId(jobId)
                .build();
    }

    @Override
    public NotificationRequestDto newJobAtFollowedCompany(UUID jobSeekerId, String companyName,
                                                          String jobTitle, UUID jobId) {
        String message = String.format("%s just posted: %s", companyName, jobTitle);

        // ALERT rather than INFO: this is the thing the seeker explicitly asked
        // to be told about, so it earns the same weight as a status change on
        // their own application.
        return NotificationRequestDto.builder()
                .recipientId(jobSeekerId)
                .message(message)
                .type("ALERT")
                .relatedEntityType("JOB")
                .relatedEntityId(jobId)
                .build();
    }

    @Override
    public NotificationRequestDto jobReportResolved(UUID reporterId, String jobTitle, boolean upheld, UUID jobId) {
        // Closing the loop matters more than it looks: someone who reports a
        // fraudulent listing and hears nothing back learns that reporting is
        // pointless, and stops.
        String message = upheld
                ? String.format("Thank you - the listing \"%s\" you reported has been removed", jobTitle)
                : String.format("We reviewed the listing \"%s\" you reported and found no breach of our rules", jobTitle);

        return NotificationRequestDto.builder()
                .recipientId(reporterId)
                .message(message)
                .type(upheld ? "ALERT" : "INFO")
                .relatedEntityType("JOB")
                .relatedEntityId(jobId)
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
