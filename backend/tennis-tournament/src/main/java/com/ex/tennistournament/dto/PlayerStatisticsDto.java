package com.ex.tennistournament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for player statistics.
 * Contains aggregated statistics for a player's performance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatisticsDto {
    private Long playerId;
    private String playerName;
    private int totalMatches;
    private int completedMatches;
    private int wins;
    private int losses;
    private int tournaments;
    private int winRate; // Percentage
}