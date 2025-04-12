package com.ex.tennistournament.observer;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.model.Match;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MatchScoreEvent represents the event data that is passed to observers
 * when a match score is updated.
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
}