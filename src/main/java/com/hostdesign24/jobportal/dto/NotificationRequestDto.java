package com.hostdesign24.jobportal.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationRequestDto(
        UUID recipientId,
        String message,
        String type,
        String relatedEntityType, //invoice, tenancy agreement, tenant, etc.
        UUID relatedEntityId //id  of the realy state entity
) {}
