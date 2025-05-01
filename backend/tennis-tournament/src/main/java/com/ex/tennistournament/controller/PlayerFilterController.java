package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.PlayerFilterDto;
import com.ex.tennistournament.dto.PlayerStatisticsDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.service.PlayerFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for filtering players with various criteria.
 * Provides endpoints for advanced player search and filtering.
 */
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerFilterController {

    private final PlayerFilterService playerFilterService;

    /**
     * Filters players based on various criteria.
     *
     * @param filterDto The filter criteria
     * @return List of players matching the criteria
     */
    @PostMapping("/filter")
    public ResponseEntity<List<UserDto>> filterPlayers(@RequestBody PlayerFilterDto filterDto) {
        return ResponseEntity.ok(playerFilterService.filterPlayers(filterDto));
    }

    /**
     * Gets a list of all players.
     *
     * @return List of all players
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllPlayers() {
        return ResponseEntity.ok(playerFilterService.getAllPlayers());
    }

    /**
     * Gets players who are registered for a specific tournament.
     *
     * @param tournamentId The tournament ID
     * @return List of players registered for the tournament
     */
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<UserDto>> getPlayersByTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(playerFilterService.getPlayersByTournament(tournamentId));
    }

    /**
     * Gets players with specified hand preference.
     *
     * @param handPreference The hand preference (RIGHT or LEFT)
     * @return List of players with the specified hand preference
     */
    @GetMapping("/hand-preference/{handPreference}")
    public ResponseEntity<List<UserDto>> getPlayersByHandPreference(@PathVariable String handPreference) {
        return ResponseEntity.ok(playerFilterService.getPlayersByHandPreference(handPreference));
    }

    /**
     * Gets statistics for a specific player.
     *
     * @param playerId The player ID
     * @return Player statistics
     */
    @GetMapping("/{playerId}/statistics")
    public ResponseEntity<PlayerStatisticsDto> getPlayerStatistics(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerFilterService.getPlayerStatistics(playerId));
    }
}