package com.hostdesign24.jobportal.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class NotificationDto {
    UUID id;
    String message;
    String type;
    LocalDateTime createdAt;
    UUID relatedEntityId;
    String relatedEntityType;
    LocalDateTime readAt;
    boolean read;
}
