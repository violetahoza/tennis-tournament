package com.ex.tennistournament.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for tournament data.
 * Represents tournament information including scheduling, status, and participants.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentDto {
    private Long id;

    @NotBlank(message = "Tournament name cannot be empty")
    private String name;

    private String description;

    @NotBlank(message = "Location cannot be empty")
    private String location;

    @NotNull(message = "Start date cannot be null")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    @NotNull(message = "End date cannot be null")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Registration deadline cannot be null")
    @Future(message = "Registration deadline must be in the future")
    private LocalDate registrationDeadline;

    @NotNull(message = "Maximum participants cannot be null")
    @Min(value = 2, message = "Minimum participants must be at least 2")
    private Integer maxParticipants;
}