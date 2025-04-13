package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.service.MatchScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing match scores.
 * Handles all score-related operations for tennis matches.
 */
@RestController
@RequestMapping("/api/match-scores")
@RequiredArgsConstructor
public class MatchScoreController {

    private final MatchScoreService matchScoreService;

    /**
     * Retrieves all scores for a specific match.
     * @param matchId ID of the match
     * @return List of scores for the match
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<MatchScoreDto>> getScoresByMatch(@PathVariable("matchId") Long matchId) {
        return ResponseEntity.ok(matchScoreService.getScoresByMatch(matchId));
    }

    /**
     * Retrieves a specific score by its ID.
     * @param id Score ID
     * @return Score details
     */
    @GetMapping("/{id}")
    public ResponseEntity<MatchScoreDto> getScoreById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchScoreService.getScoreById(id));
    }

    /**
     * Creates a new score entry. Referee access only.
     * @param scoreDto Score details
     * @return Created score
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('REFEREE', 'ADMIN')")
    public ResponseEntity<MatchScoreDto> createScore(@Valid @RequestBody MatchScoreDto scoreDto) {
        return ResponseEntity.ok(matchScoreService.createScore(scoreDto));
    }

    /**
     * Updates an existing score. Referee access only.
     * @param id Score ID
     * @param scoreDto Updated score details
     * @return Updated score
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REFEREE', 'ADMIN')")
    public ResponseEntity<MatchScoreDto> updateScore(@PathVariable("id") Long id, @Valid @RequestBody MatchScoreDto scoreDto) {
        return ResponseEntity.ok(matchScoreService.updateScore(id, scoreDto));
    }

    /**
     * Deletes a score entry. Referee access only.
     * @param id Score ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REFEREE', 'ADMIN')")
    public ResponseEntity<Void> deleteScore(@PathVariable("id") Long id) {
        matchScoreService.deleteScore(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks a match as complete. Referee access only.
     * Triggers match completion process and final score calculations.
     * @param matchId ID of the match to complete
     * @return No content on success
     */
    @PostMapping("/match/{matchId}/complete")
    @PreAuthorize("hasAnyAuthority('REFEREE', 'ADMIN')")
    public ResponseEntity<Void> completeMatch(@PathVariable("matchId") Long matchId) {
        matchScoreService.completeMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}