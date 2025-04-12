package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        // Additional comprehensive validation
        validateTournamentDates(tournamentDto);
        validateParticipants(tournamentDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentService.createTournament(tournamentDto));
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteTournament(@PathVariable("id") Long id) {
        // Check if tournament exists before attempting to delete
        tournamentService.getTournamentById(id);

        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    // Helper methods for validation
    private void validateTournamentDates(TournamentDto tournamentDto) {
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