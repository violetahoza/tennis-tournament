package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.TournamentRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentRegistrationService {

    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    public List<TournamentRegistrationDto> getRegistrationsByPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return registrationRepository.findByPlayer(player).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TournamentRegistrationDto> getRegistrationsByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return registrationRepository.findByTournament(tournament).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TournamentRegistrationDto registerPlayerForTournament(Long playerId, Long tournamentId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        if (player.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("Only players can register for tournaments");
        }

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        // Check if registration is still open
        if (tournament.getRegistrationDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Registration deadline has passed");
        }

        // Check if player is already registered
        if (registrationRepository.findByPlayerAndTournament(player, tournament).isPresent()) {
            throw new IllegalArgumentException("Player is already registered for this tournament");
        }

        // Check if tournament is full
        long registeredCount = registrationRepository.countApprovedRegistrationsByTournamentId(tournamentId);
        if (registeredCount >= tournament.getMaxParticipants()) {
            throw new IllegalArgumentException("Tournament has reached maximum participants");
        }

        TournamentRegistration registration = TournamentRegistration.builder()
                .player(player)
                .tournament(tournament)
                .registrationDate(LocalDateTime.now())
                .status(TournamentRegistration.RegistrationStatus.PENDING)
                .build();

        TournamentRegistration savedRegistration = registrationRepository.save(registration);
        return mapToDto(savedRegistration);
    }

    @Transactional
    public TournamentRegistrationDto updateRegistrationStatus(Long registrationId, TournamentRegistration.RegistrationStatus status) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        // If approving, check if tournament is full
        if (status == TournamentRegistration.RegistrationStatus.APPROVED &&
                registration.getStatus() != TournamentRegistration.RegistrationStatus.APPROVED) {

            long registeredCount = registrationRepository.countApprovedRegistrationsByTournamentId(
                    registration.getTournament().getId());

            if (registeredCount >= registration.getTournament().getMaxParticipants()) {
                throw new IllegalArgumentException("Tournament has reached maximum participants");
            }
        }

        registration.setStatus(status);
        TournamentRegistration updatedRegistration = registrationRepository.save(registration);
        return mapToDto(updatedRegistration);
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        if (!registrationRepository.existsById(registrationId)) {
            throw new ResourceNotFoundException("Registration not found with id: " + registrationId);
        }
        registrationRepository.deleteById(registrationId);
    }

    private TournamentRegistrationDto mapToDto(TournamentRegistration registration) {
        return TournamentRegistrationDto.builder()
                .id(registration.getId())
                .playerId(registration.getPlayer().getId())
                .playerName(registration.getPlayer().getFirstName() + " " + registration.getPlayer().getLastName())
                .tournamentId(registration.getTournament().getId())
                .tournamentName(registration.getTournament().getName())
                .registrationDate(registration.getRegistrationDate())
                .status(registration.getStatus())
                .build();
    }
}