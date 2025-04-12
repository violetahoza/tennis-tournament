package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.Notification;
import com.ex.tennistournament.repository.NotificationRepository;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for notification management
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Get all notifications for the current user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ex.tennistournament.model.User)) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = ((com.ex.tennistournament.model.User) auth.getPrincipal()).getId();

        // Only get notifications from database, remove in-memory storage
        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(userId);

        List<NotificationDto> dtos = notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ex.tennistournament.model.User)) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = ((com.ex.tennistournament.model.User) auth.getPrincipal()).getId();
        notificationService.markNotificationAsRead(userId, notificationId);

        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for the current user
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.ex.tennistournament.model.User)) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = ((com.ex.tennistournament.model.User) auth.getPrincipal()).getId();
        notificationService.markAllNotificationsAsRead(userId);

        return ResponseEntity.ok().build();
    }

    /**
     * Test endpoint to send a notification to a user
     * This is for testing purposes only and should be removed in production
     */
    @PostMapping("/test")
    public ResponseEntity<Void> sendTestNotification(@RequestBody NotificationDto notification) {
        notificationService.sendNotification(notification);
        return ResponseEntity.ok().build();
    }

    /**
     * Map a Notification entity to a NotificationDto
     */
    private NotificationDto mapToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setMessage(notification.getMessage());
        dto.setTimestamp(notification.getTimestamp());
        dto.setRead(notification.isRead());
        dto.setType(notification.getType());
        return dto;
    }
}