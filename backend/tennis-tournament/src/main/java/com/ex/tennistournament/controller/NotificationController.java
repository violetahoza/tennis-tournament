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
 * REST Controller for managing user notifications.
 * Handles notification retrieval and status updates.
 *
 * Features:
 * - Retrieves notifications for authenticated users
 * - Marks notifications as read (single or all)
 * - Real-time notification updates via WebSocket
 * - User-specific notification filtering
 *
 * Security:
 * - Requires authenticated user
 * - User can only access their own notifications
 * - Validates user authentication in each operation
 *
 * Integration:
 * - Works with NotificationService for WebSocket updates
 * - Uses NotificationRepository for persistence
 * - Handles notification mapping between entity and DTO
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Retrieves all notifications for the authenticated user.
     * Notifications are sorted by timestamp in descending order.
     *
     * @return ResponseEntity containing list of notifications or bad request if not authenticated
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
     * Marks a specific notification as read for the authenticated user.
     * Updates both repository and real-time notification state.
     *
     * @param notificationId ID of the notification to mark as read
     * @return ResponseEntity with no content on success or bad request if not authenticated
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
     * Marks all notifications as read for the authenticated user.
     * Updates both repository and real-time notification states.
     *
     * @return ResponseEntity with no content on success or bad request if not authenticated
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
     * Maps a Notification entity to a NotificationDto.
     * Transfers all relevant fields while maintaining data integrity.
     *
     * @param notification The notification entity to map
     * @return Mapped NotificationDto
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