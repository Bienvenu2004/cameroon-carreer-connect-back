package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.NotificationDto;
import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.exception.ActionDeniedException;
import com.hostdesign24.jobportal.exception.ResourceNotFoundException;
import com.hostdesign24.jobportal.mapper.NotificationMapper;
import com.hostdesign24.jobportal.model.Notification;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<PageResponseDto<NotificationDto>> list(@ModelAttribute FilterDto filter) {
        UUID userId = currentUserId();
        Page<Notification> page = notificationRepository
                .findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, filter.toPageable());
        List<NotificationDto> content = page.getContent().stream()
                .map(notificationMapper::toDto)
                .toList();
        return ApiResponse.success(
                new PageResponseDto<>(
                        content,
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                ),
                "Notifications retrieved"
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        long count = notificationRepository
                .countByRecipientUserIdAndReadFalseAndDeletedFalse(currentUserId());
        return ApiResponse.success(new UnreadCountResponse(count), "Unread count retrieved");
    }

    /** Mark a single notification as read. 404 if not found, 403 if it isn't yours. */
    @PatchMapping("/{id}/read")
    @Transactional
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        UUID userId = currentUserId();
        if (!userId.equals(n.getRecipientUserId())) {
            throw new ActionDeniedException("This notification does not belong to the current user");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
        return ApiResponse.success(null, "Notification marked as read");
    }

    @PatchMapping("/read-all")
    @Transactional
    public ApiResponse<Void> markAllAsRead() {
        notificationRepository.markAllReadFor(currentUserId(), LocalDateTime.now());
        return ApiResponse.success(null, "All notifications marked as read");
    }

    private static UUID currentUserId() {
        User user = Utils.getCurrentUser()
                .orElseThrow(() -> new ActionDeniedException("Authentication required"));
        return user.getId();
    }

    public record UnreadCountResponse(long count) {}
}
