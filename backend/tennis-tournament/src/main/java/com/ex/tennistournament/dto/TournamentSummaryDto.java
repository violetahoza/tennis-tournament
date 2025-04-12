package com.ex.tennistournament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for tournament summary information.
 * Provides a condensed view of tournament details for listing and overview purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentSummaryDto {
    private Long id;
    private String name;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private int registeredPlayers;
    private int maxParticipants;
    private boolean registrationOpen;
}