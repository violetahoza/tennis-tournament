package com.ex.tennistournament.dto;

import com.ex.tennistournament.model.Match;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDto {
    private Long id;

    @NotNull(message = "Tournament ID cannot be null")
    private Long tournamentId;
    private String tournamentName;

    @NotNull(message = "Player 1 ID cannot be null")
    private Long player1Id;
    private String player1Name;

    @NotNull(message = "Player 2 ID cannot be null")
    private Long player2Id;
    private String player2Name;

    @NotNull(message = "Referee ID cannot be null")
    private Long refereeId;
    private String refereeName;

    private Integer courtNumber;

    @NotNull(message = "Scheduled time cannot be null")
    private LocalDateTime scheduledTime;

    private Match.MatchStatus status;
    private Match.Round round;
}
