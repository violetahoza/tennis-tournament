package com.ex.tennistournament.dto;

import com.ex.tennistournament.model.Match;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for match summary information.
 * Provides a comprehensive overview of a tennis match including players, scores, and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSummaryDto {
    private Long matchId;
    private String tournamentName;
    private String player1Name;
    private String player2Name;
    private String refereeName;
    private LocalDateTime scheduledTime;
    private Match.MatchStatus status;
    private Match.Round round;
    private List<MatchScoreDto> scores;
    private String winner;
}