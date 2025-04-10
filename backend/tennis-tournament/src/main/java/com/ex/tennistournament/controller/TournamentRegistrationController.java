package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.TournamentRegistrationDto;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.service.TournamentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournament-registrations")
@RequiredArgsConstructor
public class TournamentRegistrationController {

    private final TournamentRegistrationService registrationService;

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByPlayer(@PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByPlayer(playerId));
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByTournament(@PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByTournament(tournamentId));
    }

    @PostMapping("/player/{playerId}/tournament/{tournamentId}")
    @PreAuthorize("hasAuthority('PLAYER')")
    public ResponseEntity<TournamentRegistrationDto> registerPlayerForTournament(
            @PathVariable("playerId") Long playerId,
            @PathVariable("tournamentId") Long tournamentId) {
        return ResponseEntity.ok(registrationService.registerPlayerForTournament(playerId, tournamentId));
    }

    @PutMapping("/{registrationId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentRegistrationDto> updateRegistrationStatus(
            @PathVariable("registrationId") Long registrationId,
            @RequestParam TournamentRegistration.RegistrationStatus status) {
        return ResponseEntity.ok(registrationService.updateRegistrationStatus(registrationId, status));
    }

    @DeleteMapping("/{registrationId}")
    public ResponseEntity<Void> cancelRegistration(@PathVariable("registrationId") Long registrationId) {
        registrationService.cancelRegistration(registrationId);
        return ResponseEntity.noContent().build();
    }
}
