package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.TournamentRegistrationDto;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.service.TournamentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing tennis tournament registrations.
 * Handles player registration, status updates, and queries.
 *
 * Security:
 * - Player role required for tournament registration
 * - Admin role required for status updates
 * - Public access for registration queries
 * - Uses @PreAuthorize for role-based authorization
 *
 * Features:
 * - Player registration for tournaments
 * - Registration status management
 * - Registration queries by player and tournament
 * - Registration cancellation
 *
 * Validations:
 * - Player eligibility for tournament
 * - Registration deadline checks
 * - Tournament capacity limits
 * - Registration status transitions
 *
 * Integration:
 * - Works with TournamentRegistrationService for business logic
 * - Uses TournamentRegistrationDto for data transfer
 * - Handles RegistrationStatus enum for state management
 */
@RestController
@RequestMapping("/api/tournament-registrations")
@RequiredArgsConstructor
public class TournamentRegistrationController {

    private final TournamentRegistrationService registrationService;

    /**
     * Retrieves all tournament registrations for a specific player.
     *
     * @param playerId ID of the player
     * @return ResponseEntity with list of player's tournament registrations
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByPlayer(@PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByPlayer(playerId));
    }

    /**
     * Retrieves all player registrations for a specific tournament.
     *
     * @param tournamentId ID of the tournament
     * @return ResponseEntity with list of tournament's player registrations
     */
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByTournament(@PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByTournament(tournamentId));
    }

    /**
     * Registers a player for a tournament. Player role required.
     * Validates registration deadline and tournament capacity.
     *
     * @param playerId ID of the player
     * @param tournamentId ID of the tournament
     * @return ResponseEntity with registration details
     */
    @PostMapping("/player/{playerId}/tournament/{tournamentId}")
    @PreAuthorize("hasAuthority('PLAYER')")
    public ResponseEntity<TournamentRegistrationDto> registerPlayerForTournament(
            @PathVariable("playerId") Long playerId,
            @PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(registrationService.registerPlayerForTournament(playerId, tournamentId));
    }

    /**
     * Updates registration status. Admin role required.
     * Validates status transition rules.
     *
     * @param registrationId ID of the registration
     * @param status New registration status
     * @return ResponseEntity with updated registration details
     */
    @PutMapping("/{registrationId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentRegistrationDto> updateRegistrationStatus(
            @PathVariable("registrationId") Long registrationId,
            @RequestParam TournamentRegistration.RegistrationStatus status) {
        return ResponseEntity.ok(registrationService.updateRegistrationStatus(registrationId, status));
    }

    /**
     * Cancels a tournament registration.
     * Validates cancellation eligibility.
     *
     * @param registrationId ID of the registration to cancel
     * @return ResponseEntity with no content on success
     */
    @DeleteMapping("/{registrationId}")
    public ResponseEntity<Void> cancelRegistration(@PathVariable("registrationId") Long registrationId) {
        registrationService.cancelRegistration(registrationId);
        return ResponseEntity.noContent().build();
    }
}
