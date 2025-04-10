package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public ResponseEntity<List<TournamentSummaryDto>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TournamentSummaryDto>> getUpcomingTournaments() {
        return ResponseEntity.ok(tournamentService.getUpcomingTournaments());
    }

    @GetMapping("/registration-open")
    public ResponseEntity<List<TournamentSummaryDto>> getOpenForRegistrationTournaments() {
        return ResponseEntity.ok(tournamentService.getOpenForRegistrationTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentDto> getTournamentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentDto> createTournament(@Valid @RequestBody TournamentDto tournamentDto) {
        return ResponseEntity.ok(tournamentService.createTournament(tournamentDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentDto> updateTournament(@PathVariable("id") Long id,
                                                          @Valid @RequestBody TournamentDto tournamentDto) {
        return ResponseEntity.ok(tournamentService.updateTournament(id, tournamentDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteTournament(@PathVariable("id") Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }
}