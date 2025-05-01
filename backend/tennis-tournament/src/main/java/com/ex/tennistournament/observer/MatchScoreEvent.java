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

    /**
     * Constructs a new MatchScoreEvent with the provided details.
     *
     * @param matchId      the unique identifier of the match
     * @param setNumber    the set number in the match
     * @param player1Score the score of player 1
     * @param player2Score the score of player 2
     * @param player1Name  the name of player 1
     * @param player2Name  the name of player 2
     * @param type         the type of event
     * @param winnerName   the name of the winner, if applicable
     */
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