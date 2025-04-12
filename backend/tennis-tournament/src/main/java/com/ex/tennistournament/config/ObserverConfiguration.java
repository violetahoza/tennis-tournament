package com.ex.tennistournament.config;

import com.ex.tennistournament.observer.MatchScoreLogger;
import com.ex.tennistournament.observer.MatchScoreNotificationService;
import com.ex.tennistournament.observer.MatchScoreSubject;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration class that sets up the Observer pattern for match score tracking.
 * Handles the registration of observers to monitor match score changes.
 *
 * Key responsibilities:
 * - Initializes and configures the Observer pattern components
 * - Registers observers to the match score subject
 * - Ensures proper setup of match score monitoring system
 *
 * Components managed:
 * - MatchScoreSubject: The subject that notifies about score changes
 * - MatchScoreLogger: Observer that logs score changes
 * - MatchScoreNotificationService: Observer that sends notifications
 */
@Configuration
@RequiredArgsConstructor
public class ObserverConfiguration {

    private final MatchScoreSubject matchScoreSubject;
    private final MatchScoreLogger matchScoreLogger;
    private final MatchScoreNotificationService matchScoreNotificationService;

    /**
     * Register observers when application context is refreshed
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Register concrete observers to the subject
        matchScoreSubject.attach(matchScoreLogger);
        matchScoreSubject.attach(matchScoreNotificationService);
    }
}