package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Tournament;
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
        validateTournamentDates(tournamentDto);

        Tournament tournament = Tournament.builder()
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
    }

    @Transactional
    public TournamentDto updateTournament(Long id, TournamentDto tournamentDto) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        validateTournamentDates(tournamentDto);

        // Check for null values before updating
        if (tournamentDto.getName() != null) {
            tournament.setName(tournamentDto.getName());
        }

        tournament.setDescription(tournamentDto.getDescription()); // Description can be null

        if (tournamentDto.getLocation() != null) {
            tournament.setLocation(tournamentDto.getLocation());
        }

        if (tournamentDto.getStartDate() != null) {
            tournament.setStartDate(tournamentDto.getStartDate());
        }

        if (tournamentDto.getEndDate() != null) {
            tournament.setEndDate(tournamentDto.getEndDate());
        }

        if (tournamentDto.getRegistrationDeadline() != null) {
            tournament.setRegistrationDeadline(tournamentDto.getRegistrationDeadline());
        }

        if (tournamentDto.getMaxParticipants() != null) {
            tournament.setMaxParticipants(tournamentDto.getMaxParticipants());
        }

        Tournament updatedTournament = tournamentRepository.save(tournament);
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

        // Delete the tournament
        tournamentRepository.deleteById(id);
    }

    private void validateTournamentDates(TournamentDto tournamentDto) {
        // Check for null dates first
        if (tournamentDto.getStartDate() == null || tournamentDto.getEndDate() == null || tournamentDto.getRegistrationDeadline() == null) {
            throw new IllegalArgumentException("All dates (start date, end date, and registration deadline) must be provided");
        }

        if (tournamentDto.getEndDate().isBefore(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (tournamentDto.getRegistrationDeadline().isAfter(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("Registration deadline cannot be after start date");
        }

        // Check if max participants is valid
        if (tournamentDto.getMaxParticipants() == null || tournamentDto.getMaxParticipants() < 2) {
            throw new IllegalArgumentException("Maximum participants must be at least 2");
        }
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
