package com.ex.tennistournament.observer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enhanced MatchScoreEvent with type information to help differentiate
 * between different types of match score events.
 */
@Getter
@RequiredArgsConstructor
public class MatchScoreEvent {
    private final Long matchId;
    private final Long setNumber;
    private final Integer player1Score;
    private final Integer player2Score;
    private final String player1Name;
    private final String player2Name;
    private final String type; // New field to specify event type

    // Constructors can be updated to include the type
    public MatchScoreEvent(
            Long matchId,
            Long setNumber,
            Integer player1Score,
            Integer player2Score,
            String player1Name,
            String player2Name
    ) {
        this(matchId, setNumber, player1Score, player2Score, player1Name, player2Name, "MATCH_SCORE");
    }
}