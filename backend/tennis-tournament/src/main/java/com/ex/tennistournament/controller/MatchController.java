package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.dto.MatchSummaryDto;
import com.ex.tennistournament.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing tennis matches.
 * Provides endpoints for CRUD operations and match-related queries.
 *
 * Security:
 * - Admin-only access for create, update, and delete operations
 * - Public access for read operations
 * - Uses @PreAuthorize for role-based authorization
 *
 * Endpoints grouped by functionality:
 * - Match retrieval (GET)
 * - Match management (POST, PUT, DELETE)
 * - Match summaries and statistics
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /**
     * Retrieves all matches in the system.
     * @return List of all matches
     */
    @GetMapping
    public ResponseEntity<List<MatchDto>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    /**
     * Retrieves all matches for a specific tournament.
     * @param tournamentId ID of the tournament
     * @return List of matches in the tournament
     */
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<MatchDto>> getMatchesByTournament(@PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(matchService.getMatchesByTournament(tournamentId));
    }

    /**
     * Retrieves all matches for a specific player.
     * @param playerId ID of the player
     * @return List of matches involving the player
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<MatchDto>> getMatchesByPlayer(@PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(matchService.getMatchesByPlayer(playerId));
    }

    /**
     * Retrieves all matches officiated by a specific referee.
     * @param refereeId ID of the referee
     * @return List of matches officiated by the referee
     */
    @GetMapping("/referee/{refereeId}")
    public ResponseEntity<List<MatchDto>> getMatchesByReferee(@PathVariable("refereeId") Long refereeId) {
        return ResponseEntity.ok(matchService.getMatchesByReferee(refereeId));
    }

    /**
     * Retrieves a specific match by its ID.
     * @param id Match ID
     * @return Match details
     */
    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatchById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    /**
     * Retrieves a summary of a specific match.
     * @param id Match ID
     * @return Match summary including scores and statistics
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<MatchSummaryDto> getMatchSummary(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchService.getMatchSummary(id));
    }

    /**
     * Creates a new match. Admin access only.
     * @param matchDto Match details
     * @return Created match
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchDto> createMatch(@Valid @RequestBody MatchDto matchDto) {
        return ResponseEntity.ok(matchService.createMatch(matchDto));
    }

    /**
     * Updates an existing match. Admin access only.
     * @param id Match ID
     * @param matchDto Updated match details
     * @return Updated match
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchDto> updateMatch(@PathVariable("id") Long id, @Valid @RequestBody MatchDto matchDto) {
        return ResponseEntity.ok(matchService.updateMatch(id, matchDto));
    }

    /**
     * Deletes a match. Admin access only.
     * @param id Match ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteMatch(@PathVariable("id") Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}