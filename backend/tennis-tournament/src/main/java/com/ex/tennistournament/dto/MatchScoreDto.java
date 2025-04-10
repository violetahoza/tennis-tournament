package com.ex.tennistournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchScoreDto {
    private Long id;

    @NotNull(message = "Match ID cannot be null")
    private Long matchId;

    @NotNull(message = "Set number cannot be null")
    @Min(value = 1, message = "Set number must be at least 1")
    private Integer setNumber;

    @NotNull(message = "Player 1 score cannot be null")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer player1Score;

    @NotNull(message = "Player 2 score cannot be null")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer player2Score;
}