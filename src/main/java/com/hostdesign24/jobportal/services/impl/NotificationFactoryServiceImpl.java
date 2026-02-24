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
}
