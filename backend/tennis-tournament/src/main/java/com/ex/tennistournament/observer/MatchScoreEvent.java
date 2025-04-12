package com.ex.tennistournament.observer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event class representing a score update in a tennis match.
 * Used in the Observer pattern to notify interested parties about score changes.
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
    private final String type;

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