package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.service.MatchScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match-scores")
@RequiredArgsConstructor
public class MatchScoreController {

    private final MatchScoreService matchScoreService;

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<MatchScoreDto>> getScoresByMatch(@PathVariable("matchId") Long matchId) {
        return ResponseEntity.ok(matchScoreService.getScoresByMatch(matchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchScoreDto> getScoreById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(matchScoreService.getScoreById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFEREE')")
    public ResponseEntity<MatchScoreDto> createScore(@Valid @RequestBody MatchScoreDto scoreDto) {
        return ResponseEntity.ok(matchScoreService.createScore(scoreDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFEREE')")
    public ResponseEntity<MatchScoreDto> updateScore(@PathVariable("id") Long id, @Valid @RequestBody MatchScoreDto scoreDto) {
        return ResponseEntity.ok(matchScoreService.updateScore(id, scoreDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFEREE')")
    public ResponseEntity<Void> deleteScore(@PathVariable("id") Long id) {
        matchScoreService.deleteScore(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/match/{matchId}/complete")
    @PreAuthorize("hasAuthority('REFEREE')")
    public ResponseEntity<Void> completeMatch(@PathVariable("matchId") Long matchId) {
        matchScoreService.completeMatch(matchId);
        return ResponseEntity.noContent().build();
    }
}
