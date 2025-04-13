package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.service.TournamentRegistrationService;
import com.ex.tennistournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing tennis tournaments.
 * Provides endpoints for tournament CRUD operations and queries.
 *
 * Security:
 * - Admin-only access for create, update, and delete operations
 * - Public access for tournament queries and retrieval
 * - Uses @PreAuthorize for role-based authorization
 *
 * Features:
 * - Tournament management (CRUD operations)
 * - Tournament status queries (upcoming, open for registration)
 * - Comprehensive input validation
 * - Date validation for tournament scheduling
 * - Participant limit validation
 *
 * Validations:
 * - Tournament dates (start, end, registration deadline)
 * - Participant limits (min 2, max 128)
 * - Request body validation using @Valid
 * - ID consistency checks for updates
 *
 * Integration:
 * - Works with TournamentService for business logic
 * - Handles both summary and detailed tournament DTOs
 * - Returns appropriate HTTP status codes
 */
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentRegistrationService registrationService;
    /**
     * Retrieves a list of all tournaments in summary format.
     * Returns basic tournament information without match details.
     *
     * @return ResponseEntity with list of tournament summaries
     */
    @GetMapping
    public ResponseEntity<List<TournamentSummaryDto>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    /**
     * Retrieves upcoming tournaments with start dates in the future.
     * Returns tournaments sorted by start date.
     *
     * @return ResponseEntity with list of upcoming tournament summaries
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<TournamentSummaryDto>> getUpcomingTournaments() {
        return ResponseEntity.ok(tournamentService.getUpcomingTournaments());
    }

    /**
     * Retrieves tournaments currently open for registration.
     * Registration is open when current date is before registration deadline.
     *
     * @return ResponseEntity with list of tournaments accepting registrations
     */
    @GetMapping("/registration-open")
    public ResponseEntity<List<TournamentSummaryDto>> getOpenForRegistrationTournaments() {
        return ResponseEntity.ok(tournamentService.getOpenForRegistrationTournaments());
    }

    /**
     * Retrieves detailed information for a specific tournament.
     *
     * @param id Tournament ID
     * @return ResponseEntity with full tournament details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TournamentDto> getTournamentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }


    /**
     * Creates a new tournament. Admin access only.
     * Validates dates and participant limits before creation.
     *
     * @param tournamentDto Tournament details
     * @return ResponseEntity with created tournament
     * @throws IllegalArgumentException if validation fails
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentDto> createTournament(@Valid @RequestBody TournamentDto tournamentDto) {
        // Additional comprehensive validation
        validateTournamentDates(tournamentDto);
        validateParticipants(tournamentDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentService.createTournament(tournamentDto));
    }

    /**
     * Updates an existing tournament. Admin access only.
     * Validates dates, participant limits, and ID consistency.
     *
     * @param id Tournament ID from path
     * @param tournamentDto Updated tournament details
     * @return ResponseEntity with updated tournament
     * @throws IllegalArgumentException if validation fails
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentDto> updateTournament(
            @PathVariable("id") Long id,
            @Valid @RequestBody TournamentDto tournamentDto) {

        // Ensure the ID in the path matches the tournament's ID
        if (tournamentDto.getId() != null && !tournamentDto.getId().equals(id)) {
            throw new IllegalArgumentException("Tournament ID in path does not match request body");
        }

        // Additional comprehensive validation
        validateTournamentDates(tournamentDto);
        validateParticipants(tournamentDto);

        return ResponseEntity.ok(tournamentService.updateTournament(id, tournamentDto));
    }

    /**
     * Deletes a tournament. Admin access only.
     * Verifies tournament exists before deletion.
     *
     * @param id Tournament ID to delete
     * @return ResponseEntity with no content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteTournament(@PathVariable("id") Long id) {
        // Check if tournament exists before attempting to delete
        tournamentService.getTournamentById(id);
        // Proceed with deletion
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants-count")
    public ResponseEntity<Map<String, Long>> getApprovedParticipantsCount(
            @PathVariable("id") Long tournamentId) {
        long count = registrationService.countApprovedRegistrationsByTournamentId(tournamentId);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    /**
     * Validates tournament dates for creation/update.
     * Checks:
     * - Start date is in future
     * - End date is after start date
     * - Registration deadline is before start and in future
     *
     * @param tournamentDto Tournament to validate
     * @throws IllegalArgumentException if dates are invalid
     */    private void validateTournamentDates(TournamentDto tournamentDto) {
        LocalDate now = LocalDate.now();

        // Check start date is in the future
        if (tournamentDto.getStartDate().isBefore(now)) {
            throw new IllegalArgumentException("Tournament start date must be in the future");
        }

        // Check end date is after start date
        if (tournamentDto.getEndDate().isBefore(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("Tournament end date must be after start date");
        }

        // Check registration deadline is before start date and not in the past
        if (tournamentDto.getRegistrationDeadline().isAfter(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("Registration deadline must be before tournament start date");
        }

        if (tournamentDto.getRegistrationDeadline().isBefore(now)) {
            throw new IllegalArgumentException("Registration deadline must be in the future");
        }
    }

    /**
     * Validates participant limits for tournament.
     * Ensures participant count is between 2 and 128.
     *
     * @param tournamentDto Tournament to validate
     * @throws IllegalArgumentException if limits are invalid
     */
    private void validateParticipants(TournamentDto tournamentDto) {
        // Validate max participants
        if (tournamentDto.getMaxParticipants() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 participants");
        }

        if (tournamentDto.getMaxParticipants() > 128) {
            throw new IllegalArgumentException("Tournament cannot have more than 128 participants");
        }
    }
}