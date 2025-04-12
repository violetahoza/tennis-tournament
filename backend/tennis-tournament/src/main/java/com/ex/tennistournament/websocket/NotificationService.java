package com.ex.tennistournament.websocket;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.Notification;
import com.ex.tennistournament.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for sending real-time notifications to users via WebSocket
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    /**
     * Sends a real-time notification to a specific user via WebSocket.
     * The notification is first persisted to the database before sending.
     *
     * Process:
     * 1. Sets timestamp if not provided
     * 2. Persists notification to database
     * 3. Updates DTO with generated ID
     * 4. Sends to WebSocket topic
     *
     * @param notification DTO containing notification details
     * @throws Exception if message processing fails
     */
    @Transactional
    public void sendNotification(NotificationDto notification) {
        try {
            // Ensure timestamp is set
            if (notification.getTimestamp() == null) {
                notification.setTimestamp(LocalDateTime.now());
            }

            // Store notification in database first
            Notification entity = new Notification();
            entity.setUserId(notification.getUserId());
            entity.setMessage(notification.getMessage());
            entity.setTimestamp(notification.getTimestamp());
            entity.setRead(notification.isRead());
            entity.setType(notification.getType());

            entity = notificationRepository.save(entity);
            notification.setId(entity.getId());

            // Send the notification via WebSocket
            String destination = "/topic/notifications/" + notification.getUserId();
            messagingTemplate.convertAndSend(destination, notification);

        } catch (Exception e) {
            log.error("Error processing notification", e);
        }
    }

    /**
     * Marks a specific notification as read for a user.
     * Only updates if notification exists and belongs to the user.
     *
     * @param userId User who owns the notification
     * @param notificationId ID of notification to mark as read
     */
    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(entity -> {
                    entity.setRead(true);
                    notificationRepository.save(entity);
                });
    }
    /**
     * Marks all unread notifications as read for a specific user.
     * Updates each notification's read status in the database.
     *
     * @param userId User whose notifications to mark as read
     */
    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        notificationRepository.findByUserIdAndReadFalse(userId)
                .forEach(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }
}