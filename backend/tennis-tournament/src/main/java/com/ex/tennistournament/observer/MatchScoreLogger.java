package com.ex.tennistournament.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Observer implementation for logging match score events.
 * Listens for match score updates and logs them to the application log.
 */
@Component
@Slf4j
public class MatchScoreLogger implements Observer {

    @Override
    public void update(String message, Object data) {
        if (data instanceof MatchScoreEvent event) {
            log(message, event);
        }
    }

    /**
     * Log match score event details
     */
    public void log(String message, MatchScoreEvent event) {
        log.info("=== MATCH SCORE EVENT ===");
        log.info("Message: {}", message);
        log.info("Match ID: {}", event.getMatchId());

        if (event.getSetNumber() != null) {
            log.info("Set Number: {}", event.getSetNumber());
        }

        log.info("Players: {} vs {}", event.getPlayer1Name(), event.getPlayer2Name());

        if (event.getPlayer1Score() != null && event.getPlayer2Score() != null) {
            log.info("Score: {} {} - {} {}",
                    event.getPlayer1Name(), event.getPlayer1Score(),
                    event.getPlayer2Score(), event.getPlayer2Name());
        }

        log.info("Logged at: {}", java.time.LocalDateTime.now());
        log.info("=======================");
    }
}