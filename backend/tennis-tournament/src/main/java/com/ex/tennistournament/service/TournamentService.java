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

        tournament.setName(tournamentDto.getName());
        tournament.setDescription(tournamentDto.getDescription());
        tournament.setLocation(tournamentDto.getLocation());
        tournament.setStartDate(tournamentDto.getStartDate());
        tournament.setEndDate(tournamentDto.getEndDate());
        tournament.setRegistrationDeadline(tournamentDto.getRegistrationDeadline());
        tournament.setMaxParticipants(tournamentDto.getMaxParticipants());

        Tournament updatedTournament = tournamentRepository.save(tournament);
        return mapToDto(updatedTournament);
    }

    @Transactional
    public void deleteTournament(Long id) {
        if (!tournamentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tournament not found with id: " + id);
        }
        tournamentRepository.deleteById(id);
    }

    private void validateTournamentDates(TournamentDto tournamentDto) {
        if (tournamentDto.getEndDate().isBefore(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (tournamentDto.getRegistrationDeadline().isAfter(tournamentDto.getStartDate())) {
            throw new IllegalArgumentException("Registration deadline cannot be after start date");
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
