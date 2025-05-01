package com.ex.tennistournament.observer;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for handling match score notifications in the tennis tournament system.
 * Implements the Observer pattern to receive and process match score events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchScoreNotificationService implements Observer {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final MatchScoreLogger matchScoreLogger;

    // Track recent notifications to prevent duplicates
    private final Map<String, LocalDateTime> recentNotifications = new HashMap<>();

    /**
     * Handles updates from the subject in the Observer pattern.
     * Processes match score events and sends notifications to players.
     *
     * @param message the message describing the update
     * @param data    the data associated with the update, expected to be a MatchScoreEvent
     */
    @Override
    public void update(String message, Object data) {
        if (data instanceof MatchScoreEvent event) {
            // Log the event
            matchScoreLogger.log(message, event);

            // Generate a unique key for this notification
            String notificationKey = generateNotificationKey(event);

            // Check if this notification is a duplicate
            if (isDuplicateNotification(notificationKey)) {
                log.info("Skipping duplicate notification: {}", notificationKey);
                return;
            }

            // Create notification message
            String notificationMessage = createNotificationMessage(message, event);

            // Determine notification type and icon
            String notificationType = determineNotificationType(message);

            // Find and notify players
            notifyPlayers(event, notificationMessage, notificationType);

            // Record this notification to prevent duplicates
            recentNotifications.put(notificationKey, LocalDateTime.now());
        }
    }

    /**
     * Generates a unique key for the notification to detect duplicates.
     *
     * @param event the match score event
     * @return a unique key based on match details and event specifics
     */
    private String generateNotificationKey(MatchScoreEvent event) {
        // Create a unique identifier based on match details and event specifics
        return String.format("%d-%d-%d-%d",
                event.getMatchId(),
                event.getSetNumber() != null ? event.getSetNumber() : -1,
                event.getPlayer1Score() != null ? event.getPlayer1Score() : -1,
                event.getPlayer2Score() != null ? event.getPlayer2Score() : -1
        );
    }

    /**
     * Checks if the notification is a duplicate.
     * Removes old entries older than 5 minutes.
     *
     * @param key the unique key for the notification
     * @return true if the notification is a duplicate, false otherwise
     */
    private boolean isDuplicateNotification(String key) {
        // Remove old entries (keep only last 5 minutes)
        recentNotifications.entrySet().removeIf(entry ->
                entry.getValue().isBefore(LocalDateTime.now().minusMinutes(5))
        );

        // Check if this key exists
        return recentNotifications.containsKey(key);
    }

    /**
     * Creates a consistent notification message based on the event type.
     *
     * @param messageType the type of the message
     * @param event       the match score event
     * @return a formatted notification message
     */
    private String createNotificationMessage(String messageType, MatchScoreEvent event) {
        String player1Name = event.getPlayer1Name();
        String player2Name = event.getPlayer2Name();

        switch (messageType) {
            case "Match score updated":
            case "New match score added":
                if (event.getSetNumber() != null &&
                        event.getPlayer1Score() != null &&
                        event.getPlayer2Score() != null) {
                    return String.format("Set %d: %s %d - %d %s",
                            event.getSetNumber(),
                            player1Name, event.getPlayer1Score(),
                            event.getPlayer2Score(), player2Name);
                }
                return String.format("Score update: %s vs %s", player1Name, player2Name);

            case "Match score deleted":
                return String.format("Set %d score removed: %s vs %s",
                        event.getSetNumber(), player1Name, player2Name);

            case "Match completed":
                // Get the actual winner from the event data
                String winner = event.getWinnerName();
                String loser = winner.equals(player1Name) ? player2Name : player1Name;
                return String.format("Match completed: %s defeats %s", winner, loser);

            default:
                return String.format("Match update: %s vs %s", player1Name, player2Name);
        }
    }

    /**
     * Determines the notification type based on the message type.
     *
     * @param messageType the type of the message
     * @return the notification type
     */
    private String determineNotificationType(String messageType) {
        switch (messageType) {
            case "Match score updated":
            case "New match score added":
                return "MATCH_SCORE";
            case "Match score deleted":
                return "MATCH_SCORE_DELETED";
            case "Match completed":
                return "MATCH_COMPLETED";
            default:
                return "MATCH_UPDATE";
        }
    }

    /**
     * Determine the clear winner of the match
     */
    private String determineClearWinner(MatchScoreEvent event) {
        // This method would ideally use the same logic as match completion in the backend
        // For simplicity, we'll use the first player name as a placeholder
        return event.getPlayer1Name();
    }

    /**
     * Notifies players involved in the match.
     *
     * @param event   the match score event
     * @param message the notification message
     * @param type    the notification type
     */
    private void notifyPlayers(MatchScoreEvent event, String message, String type) {
        // Find players by name
        User player1 = findUserByName(event.getPlayer1Name());
        User player2 = findUserByName(event.getPlayer2Name());

        // Send notifications to players
        if (player1 != null) {
            sendNotificationToUser(player1.getId(), message, type);
        }

        if (player2 != null) {
            sendNotificationToUser(player2.getId(), message, type);
        }
    }

    /**
     * Sends a notification to a specific user.
     *
     * @param userId  the ID of the user
     * @param message the notification message
     * @param type    the notification type
     */
    private void sendNotificationToUser(Long userId, String message, String type) {
        NotificationDto notification = NotificationDto.builder()
                .userId(userId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .type(type)
                .build();

        notificationService.sendNotification(notification);
    }

    /**
     * Finds a user by their full name.
     *
     * @param fullName the full name of the user
     * @return the User object if found, null otherwise
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
}