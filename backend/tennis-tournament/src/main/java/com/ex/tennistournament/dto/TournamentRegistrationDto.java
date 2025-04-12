package com.ex.tennistournament.dto;

import com.ex.tennistournament.model.TournamentRegistration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for tournament registration.
 * Used when players sign up for tournaments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRegistrationDto {
    private Long id;
    private Long playerId;
    private String playerName;
    private Long tournamentId;
    private String tournamentName;
    private LocalDateTime registrationDate;
    private TournamentRegistration.RegistrationStatus status;
}