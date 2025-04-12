package com.ex.tennistournament.config;

import com.ex.tennistournament.observer.MatchScoreLogger;
import com.ex.tennistournament.observer.MatchScoreNotificationService;
import com.ex.tennistournament.observer.MatchScoreSubject;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration class to register observers with subject
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