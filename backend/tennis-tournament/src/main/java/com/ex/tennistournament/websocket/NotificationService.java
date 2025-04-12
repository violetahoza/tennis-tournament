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

    // In-memory storage for notifications
    //private final ConcurrentMap<Long, ConcurrentMap<Long, NotificationDto>> userNotifications = new ConcurrentHashMap<>();

    // ID generator for in-memory notifications
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Sends a notification to a specific user via WebSocket
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
     * Marks a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(entity -> {
                    entity.setRead(true);
                    notificationRepository.save(entity);
                });
    }

    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        notificationRepository.findByUserIdAndReadFalse(userId)
                .forEach(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }
}