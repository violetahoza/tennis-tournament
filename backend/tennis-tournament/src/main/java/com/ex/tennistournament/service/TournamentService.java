package com.ex.tennistournament.service;

import com.ex.tennistournament.builder.TournamentBuilder;
import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;

    public List<TournamentSummaryDto> getAllTournaments() {
        return tournamentRepository.findAll().stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    public List<TournamentSummaryDto> getUpcomingTournaments() {
        return tournamentRepository.findByStartDateAfter(LocalDate.now()).stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    public List<TournamentSummaryDto> getOpenForRegistrationTournaments() {
        return tournamentRepository.findByRegistrationDeadlineAfter(LocalDate.now()).stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    public TournamentDto getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
        return mapToDto(tournament);
    }

    @Transactional
    public TournamentDto createTournament(TournamentDto tournamentDto) {
        try {
            // Use the Builder pattern to create the Tournament
            Tournament tournament = new TournamentBuilder()
                    .name(tournamentDto.getName())
                    .description(tournamentDto.getDescription())
                    .location(tournamentDto.getLocation())
                    .startDate(tournamentDto.getStartDate())
                    .endDate(tournamentDto.getEndDate())
                    .registrationDeadline(tournamentDto.getRegistrationDeadline())
                    .maxParticipants(tournamentDto.getMaxParticipants())
                    .build();

            Tournament savedTournament = tournamentRepository.save(tournament);
            return mapToDto(savedTournament);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to create tournament: " + e.getMessage());
        }
    }

    @Transactional
    public TournamentDto updateTournament(Long id, TournamentDto tournamentDto) {
        Tournament existingTournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        // Check if tournament has already started
        boolean hasStarted = existingTournament.getStartDate().isBefore(LocalDate.now()) ||
                existingTournament.getStartDate().isEqual(LocalDate.now());

        // For tournaments that have already started, only certain updates are allowed
        if (hasStarted) {
            return updateStartedTournament(existingTournament, tournamentDto);
        }

        try {
            // Use the Builder pattern to update the Tournament
            Tournament tournament = new TournamentBuilder()
                    .name(tournamentDto.getName())
                    .description(tournamentDto.getDescription())
                    .location(tournamentDto.getLocation())
                    .startDate(tournamentDto.getStartDate())
                    .endDate(tournamentDto.getEndDate())
                    .registrationDeadline(tournamentDto.getRegistrationDeadline())
                    .maxParticipants(tournamentDto.getMaxParticipants())
                    .build();

            // Set the ID from the existing tournament
            tournament.setId(existingTournament.getId());
            tournament.setCreatedAt(existingTournament.getCreatedAt());

            Tournament updatedTournament = tournamentRepository.save(tournament);
            return mapToDto(updatedTournament);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to update tournament: " + e.getMessage());
        }
    }

    /**
     * Updates limited fields for tournaments that have already started
     */
    private TournamentDto updateStartedTournament(Tournament existingTournament, TournamentDto tournamentDto) {
        // For tournaments that have already started, only allow updating description, end date, and max participants

        if (tournamentDto.getDescription() != null) {
            existingTournament.setDescription(tournamentDto.getDescription());
        }

        // Can only extend end date, not make it earlier
        if (tournamentDto.getEndDate() != null) {
            if (tournamentDto.getEndDate().isBefore(existingTournament.getEndDate())) {
                throw new IllegalArgumentException("Cannot shorten tournament duration after it has started");
            }
            existingTournament.setEndDate(tournamentDto.getEndDate());
        }

        // Can only increase max participants, not decrease
        if (tournamentDto.getMaxParticipants() != null) {
            if (tournamentDto.getMaxParticipants() < existingTournament.getMaxParticipants()) {
                throw new IllegalArgumentException("Cannot decrease maximum participants after tournament has started");
            }
            existingTournament.setMaxParticipants(tournamentDto.getMaxParticipants());
        }

        Tournament updatedTournament = tournamentRepository.save(existingTournament);
        return mapToDto(updatedTournament);
    }

    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        // Check if the tournament has already started
        if (tournament.getStartDate().isBefore(LocalDate.now()) || tournament.getStartDate().isEqual(LocalDate.now())) {
            throw new IllegalStateException("Cannot delete a tournament that has already started");
        }

        // Check if there are any matches scheduled for this tournament
        List<Match> matches = matchRepository.findByTournament(tournament);
        if (!matches.isEmpty()) {
            throw new IllegalStateException("Cannot delete a tournament that has matches scheduled. Delete the matches first.");
        }

        // Check if there are any tournament registrations
        long registrationCount = registrationRepository.countApprovedRegistrationsByTournamentId(tournament.getId());
        if (registrationCount > 0) {
            throw new IllegalStateException("Cannot delete a tournament with approved registrations. Remove the registrations first.");
        }

        // Delete the tournament
        tournamentRepository.deleteById(id);
    }

    private TournamentDto mapToDto(Tournament tournament) {
        return TournamentDto.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .description(tournament.getDescription())
                .location(tournament.getLocation())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .registrationDeadline(tournament.getRegistrationDeadline())
                .maxParticipants(tournament.getMaxParticipants())
                .build();
    }

    private TournamentSummaryDto mapToSummaryDto(Tournament tournament) {
        long registeredCount = registrationRepository.countApprovedRegistrationsByTournamentId(tournament.getId());
        boolean isOpen = tournament.getRegistrationDeadline().isAfter(LocalDate.now());

        return TournamentSummaryDto.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .location(tournament.getLocation())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .registeredPlayers((int) registeredCount)
                .maxParticipants(tournament.getMaxParticipants())
                .registrationOpen(isOpen)
                .build();
    }
}