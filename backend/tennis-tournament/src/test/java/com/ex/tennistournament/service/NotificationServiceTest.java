package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.Notification;
import com.ex.tennistournament.repository.NotificationRepository;
import com.ex.tennistournament.websocket.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link NotificationService} class.
 * This class uses Mockito to mock dependencies and test the behavior of the NotificationService.
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private NotificationRepository notificationRepository;
    @InjectMocks
    private NotificationService notificationService;

    private NotificationDto testNotification;
    private LocalDateTime fixedTime;

    /**
     * Sets up the test environment before each test.
     * Initializes test data and mocks.
     */
    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2025, 5, 1, 12, 0);

        testNotification = new NotificationDto();
        testNotification.setUserId(1L);
        testNotification.setType("MATCH_SCHEDULED");
        testNotification.setMessage("You have a new match scheduled");
        testNotification.setRead(false);
        // Deliberately not setting timestamp to test auto-setting
    }

    /**
     * Tests sending a notification and persisting it to the database.
     * Verifies that the notification is saved and sent via WebSocket.
     */
    @Test
    @DisplayName("Should send a notification and persist it to the database")
    void sendNotification_Success() {
        // Arrange
        Notification savedEntity = new Notification();
        savedEntity.setId(42L);
        savedEntity.setUserId(testNotification.getUserId());
        savedEntity.setType(testNotification.getType());
        savedEntity.setMessage(testNotification.getMessage());
        savedEntity.setRead(testNotification.isRead());
        savedEntity.setTimestamp(fixedTime);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notificationCaptor.capture())).thenReturn(savedEntity);

        // Act
        notificationService.sendNotification(testNotification);

        // Assert
        verify(notificationRepository).save(any(Notification.class));

        // Verify entity conversion
        Notification capturedNotification = notificationCaptor.getValue();
        assertEquals(testNotification.getUserId(), capturedNotification.getUserId());
        assertEquals(testNotification.getType(), capturedNotification.getType());
        assertEquals(testNotification.getMessage(), capturedNotification.getMessage());
        assertEquals(testNotification.isRead(), capturedNotification.isRead());
        assertNotNull(capturedNotification.getTimestamp(), "Timestamp should be auto-set if not provided");

        // Verify WebSocket message sending
        String expectedDestination = "/topic/notifications/" + testNotification.getUserId();
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any(NotificationDto.class));

        // Verify ID was updated in the DTO
        assertEquals(42L, testNotification.getId());
    }

    /**
     * Tests sending a notification with an existing timestamp.
     * Verifies that the provided timestamp is used.
     */
    @Test
    @DisplayName("Should use provided timestamp if available")
    void sendNotification_WithExistingTimestamp() {
        // Arrange
        testNotification.setTimestamp(fixedTime);

        Notification savedEntity = new Notification();
        savedEntity.setId(42L);
        savedEntity.setUserId(testNotification.getUserId());
        savedEntity.setType(testNotification.getType());
        savedEntity.setMessage(testNotification.getMessage());
        savedEntity.setRead(testNotification.isRead());
        savedEntity.setTimestamp(testNotification.getTimestamp());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notificationCaptor.capture())).thenReturn(savedEntity);

        // Act
        notificationService.sendNotification(testNotification);

        // Assert
        Notification capturedNotification = notificationCaptor.getValue();
        assertEquals(fixedTime, capturedNotification.getTimestamp(), "Should use the provided timestamp");
    }

    /**
     * Tests handling exceptions gracefully when sending a notification.
     * Verifies that no exception is thrown.
     */
    @Test
    @DisplayName("Should handle exceptions gracefully when sending notification")
    void sendNotification_HandlesExceptions() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> notificationService.sendNotification(testNotification));
    }

    /**
     * Tests marking a specific notification as read.
     * Verifies that the notification is updated in the database.
     */
    @Test
    @DisplayName("Should mark a specific notification as read")
    void markNotificationAsRead_Success() {
        // Arrange
        Long userId = 1L;
        Long notificationId = 42L;

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setRead(false);

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));

        // Act
        notificationService.markNotificationAsRead(userId, notificationId);

        // Assert
        assertTrue(notification.isRead(), "Notification should be marked as read");
        verify(notificationRepository).save(notification);
    }

    /**
     * Tests marking a notification as read when it does not exist.
     * Verifies that no action is taken.
     */
    @Test
    @DisplayName("Should not mark notification as read if it doesn't exist")
    void markNotificationAsRead_NotFound() {
        // Arrange
        Long userId = 1L;
        Long notificationId = 42L;

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.empty());

        // Act
        notificationService.markNotificationAsRead(userId, notificationId);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    /**
     * Tests marking all unread notifications as read for a user.
     * Verifies that all unread notifications are updated in the database.
     */
    @Test
    @DisplayName("Should mark all unread notifications as read")
    void markAllNotificationsAsRead_Success() {
        // Arrange
        Long userId = 1L;

        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUserId(userId);
        notification1.setRead(false);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(userId);
        notification2.setRead(false);

        List<Notification> unreadNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByUserIdAndReadFalse(userId))
                .thenReturn(unreadNotifications);

        // Act
        notificationService.markAllNotificationsAsRead(userId);

        // Assert
        assertTrue(notification1.isRead(), "First notification should be marked as read");
        assertTrue(notification2.isRead(), "Second notification should be marked as read");
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    /**
     * Tests marking all notifications as read when no unread notifications exist.
     * Verifies that no action is taken.
     */
    @Test
    @DisplayName("Should do nothing when no unread notifications found")
    void markAllNotificationsAsRead_NoUnread() {
        // Arrange
        Long userId = 1L;

        when(notificationRepository.findByUserIdAndReadFalse(userId))
                .thenReturn(List.of());

        // Act
        notificationService.markAllNotificationsAsRead(userId);

        // Assert
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}