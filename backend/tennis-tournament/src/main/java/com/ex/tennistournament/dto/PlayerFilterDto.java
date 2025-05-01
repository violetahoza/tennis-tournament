package com.ex.tennistournament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for filtering players.
 * Contains various criteria for player filtering.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerFilterDto {
    private String searchTerm;
    private String handPreference;
    private Long tournamentId;
    private String tournamentStatus; // APPROVED, PENDING, WAITLISTED, REJECTED
}