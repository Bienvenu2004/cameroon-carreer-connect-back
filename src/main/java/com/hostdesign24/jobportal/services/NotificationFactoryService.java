package com.hostdesign24.jobportal.services;


import com.hostdesign24.jobportal.dto.NotificationRequestDto;

import java.util.UUID;

public interface NotificationFactoryService {

    NotificationRequestDto createAccountCreationNotification(UUID userId);

    NotificationRequestDto createPasswordResetNotification(UUID userId);

    NotificationRequestDto userNewConnection(UUID userId, String device, String ip);
}
