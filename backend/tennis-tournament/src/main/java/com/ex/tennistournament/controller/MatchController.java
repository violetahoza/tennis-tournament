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

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchDto>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<MatchDto>> getMatchesByTournament(@PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(matchService.getMatchesByTournament(tournamentId));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<MatchDto>> getMatchesByPlayer(@PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(matchService.getMatchesByPlayer(playerId));
    }

    @GetMapping("/referee/{refereeId}")
    public ResponseEntity<List<MatchDto>> getMatchesByReferee(@PathVariable("refereeId") Long refereeId) {
        return ResponseEntity.ok(matchService.getMatchesByReferee(refereeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatchById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<MatchSummaryDto> getMatchSummary(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchService.getMatchSummary(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchDto> createMatch(@Valid @RequestBody MatchDto matchDto) {
        return ResponseEntity.ok(matchService.createMatch(matchDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchDto> updateMatch(@PathVariable("id") Long id, @Valid @RequestBody MatchDto matchDto) {
        return ResponseEntity.ok(matchService.updateMatch(id, matchDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteMatch(@PathVariable("id") Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}