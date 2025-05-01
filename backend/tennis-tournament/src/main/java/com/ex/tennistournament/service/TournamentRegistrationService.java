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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final EmailService emailService;

    /**
     * Retrieves all registrations for a specific player.
     *
     * @param playerId the ID of the player
     * @return a list of tournament registration DTOs
     */
    public List<TournamentRegistrationDto> getRegistrationsByPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return registrationRepository.findByPlayer(player).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all registrations for a specific tournament.
     *
     * @param tournamentId the ID of the tournament
     * @return a list of tournament registration DTOs
     */
    public List<TournamentRegistrationDto> getRegistrationsByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return registrationRepository.findByTournament(tournament).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Registers a player for a tournament.
     *
     * @param playerId     the ID of the player
     * @param tournamentId the ID of the tournament
     * @return the created tournament registration DTO
     * @throws IllegalArgumentException if the player is not eligible or registration is closed
     */
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

        notifyAdminsAboutRegistration(savedRegistration);

        return mapToDto(savedRegistration);
    }

    /**
     * Updates the status of a tournament registration.
     *
     * @param registrationId the ID of the registration
     * @param status         the new registration status
     * @return the updated tournament registration DTO
     */
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

    /**
     * Promotes waitlisted registrations to approved status if slots are available.
     *
     * @param tournamentId the ID of the tournament
     */
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

                // Send promotion notification to player
                sendRegistrationStatusChangeNotification(savedReg, oldStatus);

                // Send notification to admins about the waitlist promotion
                notifyAdminsAboutWaitlistPromotion(savedReg);
            }
        }
    }

    private void notifyAdminsAboutWaitlistPromotion(TournamentRegistration registration) {
        List<User> admins = userRepository.findByUserType(User.UserType.ADMIN);

        String playerName = registration.getPlayer().getFirstName() + " " + registration.getPlayer().getLastName();
        String tournamentName = registration.getTournament().getName();

        String message = String.format(
                "Player %s has been promoted from waitlist to APPROVED for tournament '%s'",
                playerName,
                tournamentName
        );

        // Send notification to each admin
        for (User admin : admins) {
            NotificationDto notification = NotificationDto.builder()
                    .userId(admin.getId())
                    .type("WAITLIST_PROMOTION")
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationService.sendNotification(notification);
        }
    }

    /**
     * Cancels a tournament registration.
     *
     * @param registrationId the ID of the registration to cancel
     * @throws AccessDeniedException if the user is not authorized to cancel the registration
     * @throws IllegalStateException if the tournament has already started
     */
    @Transactional
    public void cancelRegistration(Long registrationId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new AccessDeniedException("Authentication required");
        }

        User currentUser = (User) authentication.getPrincipal();

        // Check if the user is authorized to cancel this registration
        // Allow if: the user is the player who registered OR the user is an admin
        if (!currentUser.getId().equals(registration.getPlayer().getId()) &&
                currentUser.getUserType() != User.UserType.ADMIN) {
            throw new AccessDeniedException("You can only cancel your own registrations");
        }

        // Check if the tournament has already started
        if (registration.getTournament().getStartDate().isBefore(LocalDate.now()) ||
                registration.getTournament().getStartDate().isEqual(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel registration after tournament has started");
        }

        // Send notification about cancellation to player
        sendRegistrationCancellationNotification(registration);

        // Notify admins about the cancellation
        notifyAdminsAboutCancellation(registration);

        // Check if this was an approved registration to handle waitlist
        boolean wasApproved = registration.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED;

        // Delete the registration
        registrationRepository.delete(registration);

        // If this was an approved registration, promote someone from the waitlist
        if (wasApproved) {
            promoteWaitlistedRegistrations(registration.getTournament().getId());
        }
    }

    /**
     * Counts the number of approved registrations for a specific tournament.
     *
     * @param tournamentId the ID of the tournament
     * @return the count of approved registrations
     */
    public long countApprovedRegistrationsByTournamentId(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));
        return registrationRepository.countByTournamentAndStatus(
                tournament,
                TournamentRegistration.RegistrationStatus.APPROVED
        );
    }

    /**
     * Sends a notification to the player about their registration.
     *
     * @param registration the tournament registration
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

        // Send email notification
        User player = registration.getPlayer();
        String subject = "Tennis Tournament Registration Received";

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("playerName", player.getFirstName() + " " + player.getLastName());
        emailVars.put("tournamentName", tournamentName);
        emailVars.put("status", registration.getStatus().toString());
        emailVars.put("registrationDate", registration.getRegistrationDate().toString());

        emailService.sendTemplateEmail(
                player.getEmail(),
                subject,
                "registration-confirmation",
                emailVars
        );
    }

    /**
     * Sends a notification to the player about their registration.
     *
     * @param registration the tournament registration
     */
    private void sendRegistrationStatusChangeNotification(TournamentRegistration registration, TournamentRegistration.RegistrationStatus oldStatus) {
        String tournamentName = registration.getTournament().getName();
        String message;
        String type;
        String emailSubject;
        String emailTemplate;

        switch (registration.getStatus()) {
            case APPROVED:
                message = String.format(
                        "Congratulations! Your registration for tournament '%s' has been approved.",
                        tournamentName
                );
                type = "REGISTRATION_APPROVED";
                emailSubject = "Registration Approved for " + tournamentName;
                emailTemplate = "registration-approved";
                break;
            case REJECTED:
                message = String.format(
                        "We regret to inform you that your registration for tournament '%s' has been rejected.",
                        tournamentName
                );
                type = "REGISTRATION_REJECTED";
                emailSubject = "Registration Not Approved for " + tournamentName;
                emailTemplate = "registration-rejected";
                break;
            case WAITLISTED:
                message = String.format(
                        "Your registration for tournament '%s' has been placed on the waitlist due to capacity constraints.",
                        tournamentName
                );
                type = "REGISTRATION_WAITLISTED";
                emailSubject = "You've Been Waitlisted for " + tournamentName;
                emailTemplate = "registration-waitlisted";
                break;
            default:
                message = String.format(
                        "Your registration status for tournament '%s' has been updated from %s to %s.",
                        tournamentName, oldStatus, registration.getStatus()
                );
                type = "REGISTRATION_STATUS_CHANGE";
                emailSubject = "Registration Status Updated for " + tournamentName;
                emailTemplate = "registration-status-change";
        }

        NotificationDto notification = NotificationDto.builder()
                .userId(registration.getPlayer().getId())
                .type(type)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.sendNotification(notification);

        // Send email notification
        User player = registration.getPlayer();

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("playerName", player.getFirstName() + " " + player.getLastName());
        emailVars.put("tournamentName", tournamentName);
        emailVars.put("newStatus", registration.getStatus().toString());
        emailVars.put("oldStatus", oldStatus.toString());
        emailVars.put("updateDate", LocalDateTime.now().toString());

        emailService.sendTemplateEmail(
                player.getEmail(),
                emailSubject,
                emailTemplate,
                emailVars
        );
    }

    /**
     * Sends a notification to the player about registration cancellation.
     *
     * @param registration the tournament registration
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

        // Send email notification
        User player = registration.getPlayer();
        String subject = "Tournament Registration Cancelled";

        Map<String, Object> emailVars = new HashMap<>();
        emailVars.put("playerName", player.getFirstName() + " " + player.getLastName());
        emailVars.put("tournamentName", tournamentName);
        emailVars.put("cancellationDate", LocalDateTime.now().toString());

        emailService.sendTemplateEmail(
                player.getEmail(),
                subject,
                "registration-cancelled",
                emailVars
        );
    }

    /**
     * Notifies admins about a new registration.
     *
     * @param registration the tournament registration
     */
    private void notifyAdminsAboutRegistration(TournamentRegistration registration) {
        // Find all admin users
        List<User> admins = userRepository.findByUserType(User.UserType.ADMIN);

        String playerName = registration.getPlayer().getFirstName() + " " + registration.getPlayer().getLastName();
        String tournamentName = registration.getTournament().getName();
        String registrationStatus = registration.getStatus().toString();

        String message = String.format(
                "New registration: %s has registered for tournament '%s' (Status: %s)",
                playerName,
                tournamentName,
                registrationStatus
        );

        // Send notification to each admin
        for (User admin : admins) {
            NotificationDto notification = NotificationDto.builder()
                    .userId(admin.getId())
                    .type("TOURNAMENT_REGISTRATION")
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationService.sendNotification(notification);
        }
    }

    /**
     * Notifies admins about a registration cancellation.
     *
     * @param registration the tournament registration
     */
    private void notifyAdminsAboutCancellation(TournamentRegistration registration) {
        // Find all admin users
        List<User> admins = userRepository.findByUserType(User.UserType.ADMIN);

        String playerName = registration.getPlayer().getFirstName() + " " + registration.getPlayer().getLastName();
        String tournamentName = registration.getTournament().getName();
        String statusText = registration.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED ?
                "APPROVED" : registration.getStatus().toString();

        String message = String.format(
                "%s has canceled their %s registration for tournament '%s'",
                playerName,
                statusText,
                tournamentName
        );

        String notificationType = registration.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED ?
                "APPROVED_REGISTRATION_CANCELLED" : "REGISTRATION_CANCELLED";

        // Send notification to each admin
        for (User admin : admins) {
            NotificationDto notification = NotificationDto.builder()
                    .userId(admin.getId())
                    .type(notificationType)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationService.sendNotification(notification);
        }
    }

    /**
     * Maps a TournamentRegistration entity to a TournamentRegistrationDto.
     *
     * @param registration the tournament registration entity
     * @return the mapped tournament registration DTO
     */
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