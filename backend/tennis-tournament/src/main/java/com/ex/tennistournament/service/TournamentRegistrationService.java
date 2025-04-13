package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.dto.TournamentRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
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
    private final NotificationService notificationService;

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

        // Send notification to player about their registration
        sendRegistrationNotification(savedRegistration);

        return mapToDto(savedRegistration);
    }

    @Transactional
    public TournamentRegistrationDto updateRegistrationStatus(Long registrationId, TournamentRegistration.RegistrationStatus status) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        // Store old status to check if it changed
        TournamentRegistration.RegistrationStatus oldStatus = registration.getStatus();

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

        // Send notification to player if status changed
        if (oldStatus != status) {
            sendRegistrationStatusChangeNotification(updatedRegistration, oldStatus);
        }

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
            for (TournamentRegistration reg : waitlisted) {
                TournamentRegistration.RegistrationStatus oldStatus = reg.getStatus();
                reg.setStatus(TournamentRegistration.RegistrationStatus.APPROVED);
                TournamentRegistration savedReg = registrationRepository.save(reg);

                // Send promotion notification
                sendRegistrationStatusChangeNotification(savedReg, oldStatus);
            }
        }
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        // Send notification about cancellation
        sendRegistrationCancellationNotification(registration);

        registrationRepository.delete(registration);

        // If this was an approved registration, we might have space for waitlisted players
        if (registration.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED) {
            promoteWaitlistedRegistrations(registration.getTournament().getId());
        }
    }

    /**
     * Send notification to player about their initial registration
     */
    private void sendRegistrationNotification(TournamentRegistration registration) {
        String tournamentName = registration.getTournament().getName();
        String message;

        if (registration.getStatus() == TournamentRegistration.RegistrationStatus.WAITLISTED) {
            message = String.format(
                    "Your registration for tournament '%s' has been received. You are currently waitlisted.",
                    tournamentName
            );
        } else {
            message = String.format(
                    "Your registration for tournament '%s' has been received and is pending approval.",
                    tournamentName
            );
        }

        NotificationDto notification = NotificationDto.builder()
                .userId(registration.getPlayer().getId())
                .type("TOURNAMENT_REGISTRATION")
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.sendNotification(notification);
    }

    /**
     * Send notification to player about registration status change
     */
    private void sendRegistrationStatusChangeNotification(TournamentRegistration registration, TournamentRegistration.RegistrationStatus oldStatus) {
        String tournamentName = registration.getTournament().getName();
        String message;
        String type;

        switch (registration.getStatus()) {
            case APPROVED:
                message = String.format(
                        "Congratulations! Your registration for tournament '%s' has been approved.",
                        tournamentName
                );
                type = "REGISTRATION_APPROVED";
                break;
            case REJECTED:
                message = String.format(
                        "We regret to inform you that your registration for tournament '%s' has been rejected.",
                        tournamentName
                );
                type = "REGISTRATION_REJECTED";
                break;
            case WAITLISTED:
                message = String.format(
                        "Your registration for tournament '%s' has been placed on the waitlist due to capacity constraints.",
                        tournamentName
                );
                type = "REGISTRATION_WAITLISTED";
                break;
            default:
                message = String.format(
                        "Your registration status for tournament '%s' has been updated from %s to %s.",
                        tournamentName, oldStatus, registration.getStatus()
                );
                type = "REGISTRATION_STATUS_CHANGE";
        }

        NotificationDto notification = NotificationDto.builder()
                .userId(registration.getPlayer().getId())
                .type(type)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.sendNotification(notification);
    }

    /**
     * Send notification about registration cancellation
     */
    private void sendRegistrationCancellationNotification(TournamentRegistration registration) {
        String tournamentName = registration.getTournament().getName();
        String message = String.format(
                "Your registration for tournament '%s' has been cancelled.",
                tournamentName
        );

        NotificationDto notification = NotificationDto.builder()
                .userId(registration.getPlayer().getId())
                .type("REGISTRATION_CANCELLED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.sendNotification(notification);
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