package com.ex.tennistournament.observer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event class representing a score update in a tennis match.
 * Used in the Observer pattern to notify interested parties about score changes.
 */
@Getter
public class MatchScoreEvent {
    private final Long matchId;
    private final Long setNumber;
    private final Integer player1Score;
    private final Integer player2Score;
    private final String player1Name;
    private final String player2Name;
    private final String type;
    private final String winnerName;

    public MatchScoreEvent(
            Long matchId,
            Long setNumber,
            Integer player1Score,
            Integer player2Score,
            String player1Name,
            String player2Name,
            String type,
            String winnerName
    ) {
        this.matchId = matchId;
        this.setNumber = setNumber;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.type = type;
        this.winnerName = winnerName;
    }
}