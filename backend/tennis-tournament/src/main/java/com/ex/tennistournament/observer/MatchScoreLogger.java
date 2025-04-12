package com.ex.tennistournament.observer;

import com.ex.tennistournament.model.Match;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MatchScoreLogger is a concrete observer that logs match score updates.
 * Enhanced with detailed console logging to demonstrate the Observer pattern in action.
 */
@Component
@Slf4j
public class MatchScoreLogger implements Observer {
    @Override
    public void update(String message, Object data) {
        if (data instanceof MatchScoreEvent event) {
            // Log a separator line for visibility
            log.info("=================================================================");
            log.info("OBSERVER NOTIFICATION RECEIVED: {}", message);

            // Log match information
            log.info("Match ID: {}", event.getMatchId());

            // Log set information if available
            if (event.getSetNumber() != null) {
                log.info("Set: {}", event.getSetNumber());
            }

            // Log player information
            log.info("Players: {} vs {}", event.getPlayer1Name(), event.getPlayer2Name());

            // Log score information if available
            if (event.getPlayer1Score() != null && event.getPlayer2Score() != null) {
                log.info("Score: {} {} - {} {}",
                        event.getPlayer1Name(), event.getPlayer1Score(),
                        event.getPlayer2Score(), event.getPlayer2Name());
            }

            // Log the current time to show when this notification was processed
            log.info("Notification processed at: {}", java.time.LocalDateTime.now());
            log.info("=================================================================");

            // Also print to console for immediate visibility during testing
            System.out.println("=================================================================");
            System.out.println("OBSERVER NOTIFICATION: " + message);
            System.out.println("Match ID: " + event.getMatchId());

            if (event.getSetNumber() != null) {
                System.out.println("Set: " + event.getSetNumber());
            }

            System.out.println("Players: " + event.getPlayer1Name() + " vs " + event.getPlayer2Name());

            if (event.getPlayer1Score() != null && event.getPlayer2Score() != null) {
                System.out.println("Score: " + event.getPlayer1Name() + " " + event.getPlayer1Score() +
                        " - " + event.getPlayer2Score() + " " + event.getPlayer2Name());
            }

            System.out.println("Notification processed at: " + java.time.LocalDateTime.now());
            System.out.println("=================================================================");
        } else {
            // Log if we receive an unexpected data format
            log.warn("Received notification with unexpected data format: {}", data.getClass().getName());
            System.out.println("Received notification with unexpected data format: " + data.getClass().getName());
        }
    }
}