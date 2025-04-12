package com.ex.tennistournament.observer;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * A service that sends notifications to users when match scores are updated.
 * This observer implementation delivers notifications to users via WebSocket.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchScoreNotificationService implements Observer {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Override
    public void update(String message, Object data) {
        if (data instanceof MatchScoreEvent event) {
            log.info("🔔 NOTIFICATION SERVICE: {}", message);

            // Get player names for the notification
            String player1Name = event.getPlayer1Name();
            String player2Name = event.getPlayer2Name();

            // Create notification message based on the event type
            String notificationMessage = createNotificationMessage(message, event);

            // Log the notification for debugging
            log.info("Notification message: {}", notificationMessage);

            // Find relevant users to notify (players involved in the match)
            User player1 = findUserByName(player1Name);
            User player2 = findUserByName(player2Name);

            // Create notification objects
            if (player1 != null) {
                sendNotificationToUser(player1.getId(), notificationMessage);
            }

            if (player2 != null) {
                sendNotificationToUser(player2.getId(), notificationMessage);
            }
        }
    }

    /**
     * Creates an appropriate notification message based on the event type
     */
    private String createNotificationMessage(String messageType, MatchScoreEvent event) {
        String player1Name = event.getPlayer1Name();
        String player2Name = event.getPlayer2Name();

        if (messageType.contains("added")) {
            if (event.getPlayer1Score() != null && event.getPlayer2Score() != null) {
                return String.format("Set %d score recorded: %s %d - %d %s",
                        event.getSetNumber(), player1Name, event.getPlayer1Score(),
                        event.getPlayer2Score(), player2Name);
            } else {
                return String.format("New score recorded for match: %s vs %s",
                        player1Name, player2Name);
            }
        } else if (messageType.contains("updated")) {
            if (event.getPlayer1Score() != null && event.getPlayer2Score() != null) {
                return String.format("Score updated in Set %d: %s %d - %d %s",
                        event.getSetNumber(), player1Name, event.getPlayer1Score(),
                        event.getPlayer2Score(), player2Name);
            } else {
                return String.format("Score updated for match: %s vs %s",
                        player1Name, player2Name);
            }
        } else if (messageType.contains("deleted")) {
            return String.format("Score for Set %d was removed in match: %s vs %s",
                    event.getSetNumber(), player1Name, player2Name);
        } else if (messageType.contains("completed")) {
            return String.format("Match completed: %s vs %s. Check final results!",
                    player1Name, player2Name);
        } else {
            return String.format("Update in match: %s vs %s",
                    player1Name, player2Name);
        }
    }

    /**
     * Finds a user by their full name
     */
    private User findUserByName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }

        String[] nameParts = fullName.split(" ");
        if (nameParts.length < 2) {
            log.warn("Could not parse name: {}", fullName);
            return null;
        }

        String firstName = nameParts[0];
        String lastName = nameParts[nameParts.length - 1];

        return userRepository.findByFirstNameAndLastName(firstName, lastName).orElse(null);
    }

    /**
     * Sends a notification to a specific user
     */
    private void sendNotificationToUser(Long userId, String message) {
        NotificationDto notification = NotificationDto.builder()
                .userId(userId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .type("MATCH_SCORE")
                .build();

        notificationService.sendNotification(notification);
    }
}