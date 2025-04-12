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

/**
 * Service class for managing tournament registrations.
 * Handles player registration workflows and waitlist management.
 */
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
        TournamentRegistration.RegistrationStatus initialStatus =
                registeredCount >= tournament.getMaxParticipants()
                        ? TournamentRegistration.RegistrationStatus.WAITLISTED
                        : TournamentRegistration.RegistrationStatus.PENDING;

        TournamentRegistration registration = TournamentRegistration.builder()
                .player(player)
                .tournament(tournament)
                .registrationDate(LocalDateTime.now())
                .status(initialStatus)
                .build();

        TournamentRegistration savedRegistration = registrationRepository.save(registration);
        return mapToDto(savedRegistration);
    }

    @Transactional
    public TournamentRegistrationDto updateRegistrationStatus(Long registrationId, TournamentRegistration.RegistrationStatus status) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        // If approving, check if tournament is full
        if (status == TournamentRegistration.RegistrationStatus.APPROVED) {
            long registeredCount = registrationRepository.countApprovedRegistrationsByTournamentId(
                    registration.getTournament().getId());

            if (registeredCount >= registration.getTournament().getMaxParticipants()) {
                // Automatically set to WAITLISTED if tournament is full
                status = TournamentRegistration.RegistrationStatus.WAITLISTED;
            }
        }

        registration.setStatus(status);
        TournamentRegistration updatedRegistration = registrationRepository.save(registration);

        // If this registration was approved, check if we can promote any waitlisted registrations
        if (status == TournamentRegistration.RegistrationStatus.APPROVED) {
            promoteWaitlistedRegistrations(registration.getTournament().getId());
        }

        return mapToDto(updatedRegistration);
    }

    protected void promoteWaitlistedRegistrations(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        long approvedCount = registrationRepository.countApprovedRegistrationsByTournamentId(tournamentId);
        int availableSlots = tournament.getMaxParticipants() - (int) approvedCount;

        if (availableSlots > 0) {
            // Get the oldest waitlisted registrations (up to available slots)
            List<TournamentRegistration> waitlisted = registrationRepository
                    .findTopNByTournamentIdAndStatusOrderByRegistrationDateAsc(
                            tournamentId,
                            TournamentRegistration.RegistrationStatus.WAITLISTED,
                            availableSlots);

            // Approve these registrations
            waitlisted.forEach(reg -> {
                reg.setStatus(TournamentRegistration.RegistrationStatus.APPROVED);
                registrationRepository.save(reg);
            });
        }
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        registrationRepository.delete(registration);

        // If this was an approved registration, we might have space for waitlisted players
        if (registration.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED) {
            promoteWaitlistedRegistrations(registration.getTournament().getId());
        }
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