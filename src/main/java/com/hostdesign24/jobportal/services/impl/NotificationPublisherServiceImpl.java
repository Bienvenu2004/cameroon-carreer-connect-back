package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.dto.NotificationDto;
import com.hostdesign24.jobportal.dto.NotificationRequestDto;
import com.hostdesign24.jobportal.exception.NotificationDeliveryException;
import com.hostdesign24.jobportal.mapper.NotificationMapper;
import com.hostdesign24.jobportal.model.Notification;
import com.hostdesign24.jobportal.repository.NotificationRepository;
import com.hostdesign24.jobportal.services.NotificationPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.hostdesign24.jobportal.common.constants.socket.WebSocketConstants.NOTIFICATION_QUE;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisherServiceImpl implements NotificationPublisherService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    @Transactional
    public NotificationDto persistAndPublish(NotificationRequestDto request) {
        if (request == null || request.message() == null) {
            log.warn("Cannot create notification with null request or message");
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipientUserId(request.recipientId());
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setRelatedEntityType(request.relatedEntityType());
        notification.setRelatedEntityId(request.relatedEntityId());
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = notificationMapper.toDto(saved);
        
        publishToWebSocket(request.recipientId(), dto);
        
        return dto;
    }

    private void publishToWebSocket(UUID userId, NotificationDto dto) {
        if (userId == null) {
            log.error("Cannot publish message: user ID is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (dto == null) {
            log.error("Cannot publish message to user {}: notification DTO is null", userId);
            throw new IllegalArgumentException("Notification DTO cannot be null");
        }

        try {
            simpMessagingTemplate.convertAndSendToUser(userId.toString(), NOTIFICATION_QUE, dto);
            log.info("Real-time notification sent to user {}", userId);
        } catch (MessageDeliveryException e) {
            log.error("Failed to deliver message to user {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending notification: {}", e.getMessage());
            throw new NotificationDeliveryException("Failed to deliver notification", e);
        }
    }
}
