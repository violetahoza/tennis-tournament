package com.ex.tennistournament.service;

import com.ex.tennistournament.builder.MatchBuilder;
import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.dto.MatchSummaryDto;
import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing tennis matches in the tournament system.
 * Handles CRUD operations for matches and enforces business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // Date formatters for consistent formatting
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Retrieves all matches in the system.
     *
     * @return a list of match DTOs
     */
    public List<MatchDto> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all matches for a specific tournament.
     *
     * @param tournamentId the ID of the tournament
     * @return a list of match DTOs
     */
    public List<MatchDto> getMatchesByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return matchRepository.findByTournament(tournament).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all matches for a specific player.
     *
     * @param playerId the ID of the player
     * @return a list of match DTOs
     */
    public List<MatchDto> getMatchesByPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return matchRepository.findByPlayer1OrPlayer2(player, player).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all matches assigned to a specific referee.
     *
     * @param refereeId the ID of the referee
     * @return a list of match DTOs
     */
    public List<MatchDto> getMatchesByReferee(Long refereeId) {
        User referee = userRepository.findById(refereeId)
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + refereeId));

        return matchRepository.findByReferee(referee).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific match by its ID.
     *
     * @param id the ID of the match
     * @return the match DTO
     */
    public MatchDto getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return mapToDto(match);
    }

    /**
     * Retrieves a summary of a specific match, including scores and winner.
     *
     * @param id the ID of the match
     * @return the match summary DTO
     */
    public MatchSummaryDto getMatchSummary(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));

        List<MatchScore> scores = matchScoreRepository.findByMatchOrderBySetNumber(match);

        String winnerName = determineWinner(scores, match);

        return MatchSummaryDto.builder()
                .matchId(match.getId())
                .tournamentName(match.getTournament().getName())
                .player1Name(match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName())
                .player2Name(match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName())
                .refereeName(match.getReferee().getFirstName() + " " + match.getReferee().getLastName())
                .scheduledTime(match.getScheduledTime())
                .status(match.getStatus())
                .round(match.getRound())
                .scores(scores.stream().map(this::mapScoreToDto).collect(Collectors.toList()))
                .winner(winnerName)
                .build();
    }

    /**
     * Creates a new match in the system.
     *
     * @param matchDto the match DTO containing match details
     * @return the created match DTO
     */
    @Transactional
    public MatchDto createMatch(MatchDto matchDto) {
        Tournament tournament = tournamentRepository.findById(matchDto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + matchDto.getTournamentId()));

        User player1 = userRepository.findById(matchDto.getPlayer1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 1 not found with id: " + matchDto.getPlayer1Id()));

        User player2 = userRepository.findById(matchDto.getPlayer2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 2 not found with id: " + matchDto.getPlayer2Id()));

        User referee = userRepository.findById(matchDto.getRefereeId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

        // Validate that players are registered for the tournament
        validatePlayerRegistration(player1, tournament);
        validatePlayerRegistration(player2, tournament);

        // Validate match timing
        validateMatchTiming(matchDto.getScheduledTime(), tournament);

        // Use the Builder pattern to create the Match
        try {
            Match match = new MatchBuilder()
                    .tournament(tournament)
                    .player1(player1)
                    .player2(player2)
                    .referee(referee)
                    .courtNumber(matchDto.getCourtNumber())
                    .scheduledTime(matchDto.getScheduledTime())
                    .status(Match.MatchStatus.SCHEDULED)
                    .round(matchDto.getRound())
                    .build();

            Match savedMatch = matchRepository.save(match);

            // Send notifications to all parties
            sendMatchScheduledNotifications(savedMatch);

            return mapToDto(savedMatch);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to create match: " + e.getMessage());
        }
    }

    @Transactional
    public MatchDto updateMatch(Long id, MatchDto matchDto) {
        Match existingMatch = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));

        // If match is already in progress or completed, limit what can be changed
        if (existingMatch.getStatus() == Match.MatchStatus.IN_PROGRESS ||
                existingMatch.getStatus() == Match.MatchStatus.COMPLETED) {

            // For in-progress or completed matches, only allow updating certain fields
            return updateLimitedMatchFields(existingMatch, matchDto);
        }

        Tournament tournament = tournamentRepository.findById(matchDto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + matchDto.getTournamentId()));

        User player1 = userRepository.findById(matchDto.getPlayer1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 1 not found with id: " + matchDto.getPlayer1Id()));

        User player2 = userRepository.findById(matchDto.getPlayer2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 2 not found with id: " + matchDto.getPlayer2Id()));

        User referee = userRepository.findById(matchDto.getRefereeId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

        // Check if referee has changed - we need this to send a notification if necessary
        boolean refereeChanged = !existingMatch.getReferee().getId().equals(referee.getId());

        // Validate that players are registered for the tournament
        validatePlayerRegistration(player1, tournament);
        validatePlayerRegistration(player2, tournament);

        // Validate match timing
        validateMatchTiming(matchDto.getScheduledTime(), tournament);

        // Save the current status for comparison after update
        Match.MatchStatus previousStatus = existingMatch.getStatus();

        // Use the Builder pattern to update the Match
        try {
            // Start with default values from existing match
            Match updatedMatch = new MatchBuilder()
                    .tournament(tournament)
                    .player1(player1)
                    .player2(player2)
                    .referee(referee)
                    .courtNumber(matchDto.getCourtNumber())
                    .scheduledTime(matchDto.getScheduledTime())
                    .status(matchDto.getStatus())
                    .round(matchDto.getRound())
                    .build();

            // Set the ID from the existing match
            updatedMatch.setId(existingMatch.getId());
            updatedMatch.setCreatedAt(existingMatch.getCreatedAt());

            Match savedMatch = matchRepository.save(updatedMatch);

            // Send notifications if status changed
            if (previousStatus != savedMatch.getStatus()) {
                sendStatusChangeNotifications(savedMatch, previousStatus);
            }

            // Send notification if referee changed
            if (refereeChanged) {
                // Notify new referee of assignment
                sendRefereeAssignmentNotifications(savedMatch);

                // Notify old referee of removal
                sendRefereeReassignmentNotification(existingMatch.getReferee(), savedMatch);
            }

            // Send notification for schedule changes if time changed but status remained SCHEDULED
            if (previousStatus == Match.MatchStatus.SCHEDULED &&
                    savedMatch.getStatus() == Match.MatchStatus.SCHEDULED &&
                    !existingMatch.getScheduledTime().equals(savedMatch.getScheduledTime())) {

                sendScheduleChangeNotifications(savedMatch, existingMatch.getScheduledTime());
            }

            return mapToDto(savedMatch);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to update match: " + e.getMessage());
        }
    }

    /**
     * Updates only certain fields of a match that is already in progress or completed.
     *
     * @param existingMatch The existing match to update
     * @param matchDto The data to update
     * @return The updated match DTO
     */
    private MatchDto updateLimitedMatchFields(Match existingMatch, MatchDto matchDto) {
        // Save current status for comparison
        Match.MatchStatus previousStatus = existingMatch.getStatus();
        User previousReferee = existingMatch.getReferee();

        // For in-progress matches, only allow updating the status, court number, and referee
        if (existingMatch.getStatus() == Match.MatchStatus.IN_PROGRESS) {
            // Can only update to COMPLETED or CANCELLED
            if (matchDto.getStatus() != Match.MatchStatus.IN_PROGRESS &&
                    matchDto.getStatus() != Match.MatchStatus.COMPLETED &&
                    matchDto.getStatus() != Match.MatchStatus.CANCELLED) {
                throw new IllegalArgumentException("In-progress match can only be updated to COMPLETED or CANCELLED");
            }

            // Allow changing referee
            boolean refereeChanged = false;
            if (!existingMatch.getReferee().getId().equals(matchDto.getRefereeId())) {
                User newReferee = userRepository.findById(matchDto.getRefereeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

                if (newReferee.getUserType() != User.UserType.REFEREE) {
                    throw new IllegalArgumentException("Referee must be a referee");
                }

                existingMatch.setReferee(newReferee);
                refereeChanged = true;
            }

            // Allow changing court number
            if (matchDto.getCourtNumber() != null) {
                existingMatch.setCourtNumber(matchDto.getCourtNumber());
            }

            existingMatch.setStatus(matchDto.getStatus());

            Match savedMatch = matchRepository.save(existingMatch);

            // If status changed, send notifications to all parties
            if (previousStatus != savedMatch.getStatus()) {
                sendStatusChangeNotifications(savedMatch, previousStatus);
            }

            // If referee was changed, notify both the old and new referee
            if (refereeChanged) {
                sendRefereeAssignmentNotifications(savedMatch);
                sendRefereeReassignmentNotification(previousReferee, savedMatch);
            }

            return mapToDto(savedMatch);

        } else if (existingMatch.getStatus() == Match.MatchStatus.COMPLETED) {
            // For completed matches, only allow changing to CANCELLED
            if (matchDto.getStatus() == Match.MatchStatus.CANCELLED) {
                existingMatch.setStatus(Match.MatchStatus.CANCELLED);
                Match savedMatch = matchRepository.save(existingMatch);

                // Send cancellation notifications
                sendMatchCancelledNotifications(savedMatch);

                return mapToDto(savedMatch);
            } else if (matchDto.getStatus() != Match.MatchStatus.COMPLETED) {
                throw new IllegalArgumentException("Completed match can only be updated to CANCELLED");
            }
        }

        // If we reach here, nothing changed that requires notifications
        Match savedMatch = matchRepository.save(existingMatch);
        return mapToDto(savedMatch);
    }

    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));

        // Only allow deleting matches that are SCHEDULED or CANCELLED
        if (match.getStatus() != Match.MatchStatus.SCHEDULED && match.getStatus() != Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Only scheduled or cancelled matches can be deleted");
        }

        // Check if the match has any recorded scores
        List<MatchScore> scores = matchScoreRepository.findByMatch(match);
        if (!scores.isEmpty()) {
            throw new IllegalStateException("Cannot delete a match that has recorded scores");
        }

        // Send cancellation notification if not already cancelled
        if (match.getStatus() != Match.MatchStatus.CANCELLED) {
            sendMatchCancelledNotifications(match);
        }

        matchRepository.deleteById(id);
    }

    /**
     * Sends notifications to players about a newly scheduled match
     */
    private void sendMatchScheduledNotifications(Match match) {
        log.info("Sending match scheduled notifications for match ID: {}", match.getId());

        String formattedTime = match.getScheduledTime().format(DATE_TIME_FORMATTER);

        // Player 1 notification
        String message1 = String.format("New match scheduled against %s at %s on court %d",
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                formattedTime,
                match.getCourtNumber());

        sendPlayerNotification(match.getPlayer1(), "MATCH_SCHEDULED", message1);
        sendMatchScheduledEmail(match, match.getPlayer1(), match.getPlayer2());

        // Player 2 notification
        String message2 = String.format("New match scheduled against %s at %s on court %d",
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                formattedTime,
                match.getCourtNumber());

        sendPlayerNotification(match.getPlayer2(), "MATCH_SCHEDULED", message2);
        sendMatchScheduledEmail(match, match.getPlayer2(), match.getPlayer1());

        // Also notify the referee about this assignment
        sendRefereeAssignmentNotifications(match);
    }

    /**
     * Sends notification to a referee when they are assigned to a match
     */
    private void sendRefereeAssignmentNotifications(Match match) {
        log.info("Sending referee assignment notification for match ID: {}", match.getId());

        User referee = match.getReferee();

        if (referee == null) {
            log.warn("Cannot send referee assignment notification - referee is null for match ID: {}", match.getId());
            return;
        }

        String formattedDateTime = match.getScheduledTime().format(DATE_TIME_FORMATTER);

        String message = String.format(
                "You have been assigned as referee for the match: %s vs %s at %s on court %d",
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                formattedDateTime,
                match.getCourtNumber()
        );

        // Send in-app notification
        sendRefereeNotification(referee, "MATCH_ASSIGNMENT", message);

        // Send email notification
        sendRefereeAssignmentEmail(match, referee, true);
    }

    /**
     * Sends notification to a referee when they are removed from a match
     */
    private void sendRefereeReassignmentNotification(User previousReferee, Match match) {
        log.info("Sending referee reassignment notification to previous referee for match ID: {}", match.getId());

        if (previousReferee == null) {
            log.warn("Cannot send referee reassignment notification - previous referee is null for match ID: {}", match.getId());
            return;
        }

        String message = String.format(
                "You have been removed as referee from the match: %s vs %s",
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
        );

        // Send in-app notification
        sendRefereeNotification(previousReferee, "MATCH_ASSIGNMENT_REMOVED", message);

        // Send email notification
        try {
            String subject = "Referee Assignment Removed - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("refereeName", previousReferee.getFirstName() + " " + previousReferee.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("matchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("matchTime", match.getScheduledTime().format(TIME_FORMATTER));

            emailService.sendTemplateEmail(
                    previousReferee.getEmail(),
                    subject,
                    "referee-reassignment",
                    emailVars
            );

            log.info("Sent referee reassignment email to: {}", previousReferee.getEmail());
        } catch (Exception e) {
            log.error("Failed to send referee reassignment email: {}", e.getMessage());
        }
    }

    /**
     * Send notifications to all relevant parties when a match status changes
     */
    private void sendStatusChangeNotifications(Match match, Match.MatchStatus previousStatus) {
        log.info("Sending status change notifications for match ID: {}, status change from {} to {}",
                match.getId(), previousStatus, match.getStatus());

        String statusChangeMsg = String.format(
                "Match status changed from %s to %s for match: %s vs %s",
                previousStatus,
                match.getStatus(),
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
        );

        // If match was cancelled, use specific cancellation notifications
        if (match.getStatus() == Match.MatchStatus.CANCELLED) {
            sendMatchCancelledNotifications(match);
            return;
        }

        // Send to player 1
        sendPlayerNotification(match.getPlayer1(), "MATCH_STATUS_CHANGE", statusChangeMsg);
        sendStatusChangeEmail(match, match.getPlayer1(), previousStatus);

        // Send to player 2
        sendPlayerNotification(match.getPlayer2(), "MATCH_STATUS_CHANGE", statusChangeMsg);
        sendStatusChangeEmail(match, match.getPlayer2(), previousStatus);

        // Send to referee
        sendRefereeNotification(match.getReferee(), "MATCH_STATUS_CHANGE", statusChangeMsg);
        sendStatusChangeEmail(match, match.getReferee(), previousStatus);
    }

    /**
     * Send notifications when a match's schedule changes
     */
    private void sendScheduleChangeNotifications(Match match, LocalDateTime previousTime) {
        log.info("Sending schedule change notifications for match ID: {}", match.getId());

        String previousTimeStr = previousTime.format(DATE_TIME_FORMATTER);
        String newTimeStr = match.getScheduledTime().format(DATE_TIME_FORMATTER);

        String message = String.format(
                "Match schedule has changed from %s to %s for match: %s vs %s on court %d",
                previousTimeStr,
                newTimeStr,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                match.getCourtNumber()
        );

        // Notify player 1
        sendPlayerNotification(match.getPlayer1(), "MATCH_RESCHEDULED", message);
        sendScheduleChangeEmail(match, match.getPlayer1(), previousTime);

        // Notify player 2
        sendPlayerNotification(match.getPlayer2(), "MATCH_RESCHEDULED", message);
        sendScheduleChangeEmail(match, match.getPlayer2(), previousTime);

        // Notify referee
        sendRefereeNotification(match.getReferee(), "MATCH_RESCHEDULED", message);
        sendScheduleChangeEmail(match, match.getReferee(), previousTime);
    }

    /**
     * Send notifications to all relevant parties when a match is cancelled
     */
    private void sendMatchCancelledNotifications(Match match) {
        log.info("Sending match cancellation notifications for match ID: {}", match.getId());

        String formattedTime = match.getScheduledTime().format(DATE_TIME_FORMATTER);

        // Player notifications
        String playerMessage = String.format(
                "Your match scheduled for %s has been cancelled: %s vs %s",
                formattedTime,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
        );

        // Notify player 1
        sendPlayerNotification(match.getPlayer1(), "MATCH_CANCELLED", playerMessage);
        sendMatchCancellationEmail(match, match.getPlayer1(), true);

        // Notify player 2
        sendPlayerNotification(match.getPlayer2(), "MATCH_CANCELLED", playerMessage);
        sendMatchCancellationEmail(match, match.getPlayer2(), true);

        // Referee notification
        String refereeMessage = String.format(
                "Match you were assigned to referee has been cancelled: %s vs %s (scheduled for %s)",
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                formattedTime
        );

        sendRefereeNotification(match.getReferee(), "MATCH_CANCELLED", refereeMessage);
        sendMatchCancellationEmail(match, match.getReferee(), false);
    }

    /**
     * Helper method to send a notification to a player
     */
    private void sendPlayerNotification(User player, String type, String message) {
        if (player == null) {
            log.warn("Cannot send player notification - player is null");
            return;
        }

        try {
            NotificationDto notification = NotificationDto.builder()
                    .userId(player.getId())
                    .type(type)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationService.sendNotification(notification);
            log.debug("Sent {} notification to player {}", type, player.getUsername());
        } catch (Exception e) {
            log.error("Failed to send notification to player {}: {}", player.getUsername(), e.getMessage());
        }
    }

    /**
     * Helper method to send a notification to a referee
     */
    private void sendRefereeNotification(User referee, String type, String message) {
        if (referee == null) {
            log.warn("Cannot send referee notification - referee is null");
            return;
        }

        try {
            NotificationDto notification = NotificationDto.builder()
                    .userId(referee.getId())
                    .type(type)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notificationService.sendNotification(notification);
            log.debug("Sent {} notification to referee {}", type, referee.getUsername());
        } catch (Exception e) {
            log.error("Failed to send notification to referee {}: {}", referee.getUsername(), e.getMessage());
        }
    }

    /**
     * Sends an email notification to a player about a scheduled match
     */
    private void sendMatchScheduledEmail(Match match, User recipient, User opponent) {
        try {
            String subject = "New Match Scheduled - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("recipientName", recipient.getFirstName() + " " + recipient.getLastName());
            emailVars.put("opponentName", opponent.getFirstName() + " " + opponent.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("matchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("matchTime", match.getScheduledTime().format(TIME_FORMATTER));
            emailVars.put("courtNumber", "Court " + match.getCourtNumber());
            emailVars.put("matchRound", match.getRound().toString().replace("_", " "));

            emailService.sendTemplateEmail(
                    recipient.getEmail(),
                    subject,
                    "match-scheduled",
                    emailVars
            );

            log.info("Sent match scheduled email to: {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send match scheduled email: {}", e.getMessage());
        }
    }

    /**
     * Sends an email notification to a referee about a match assignment
     */
    private void sendRefereeAssignmentEmail(Match match, User referee, boolean isNewAssignment) {
        try {
            String subject = isNewAssignment ?
                    "New Match Assignment - Tennis Tournament" :
                    "Updated Match Assignment - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("refereeName", referee.getFirstName() + " " + referee.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("matchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("matchTime", match.getScheduledTime().format(TIME_FORMATTER));
            emailVars.put("courtNumber", "Court " + match.getCourtNumber());
            emailVars.put("matchRound", match.getRound().toString().replace("_", " "));

            emailService.sendTemplateEmail(
                    referee.getEmail(),
                    subject,
                    "referee-assignment",
                    emailVars
            );

            log.info("Sent referee assignment email to: {}", referee.getEmail());
        } catch (Exception e) {
            log.error("Failed to send referee assignment email: {}", e.getMessage());
        }
    }

    /**
     * Sends an email about a match status change
     */
    private void sendStatusChangeEmail(Match match, User recipient, Match.MatchStatus previousStatus) {
        try {
            String subject = "Match Status Update - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("recipientName", recipient.getFirstName() + " " + recipient.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("matchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("matchTime", match.getScheduledTime().format(TIME_FORMATTER));
            emailVars.put("previousStatus", previousStatus.toString());
            emailVars.put("newStatus", match.getStatus().toString());

            emailService.sendTemplateEmail(
                    recipient.getEmail(),
                    subject,
                    "match-status-change",
                    emailVars
            );

            log.info("Sent match status change email to: {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send match status change email: {}", e.getMessage());
        }
    }

    /**
     * Sends an email about a match schedule change
     */
    private void sendScheduleChangeEmail(Match match, User recipient, LocalDateTime previousTime) {
        try {
            String subject = "Match Schedule Update - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("recipientName", recipient.getFirstName() + " " + recipient.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("oldMatchDate", previousTime.format(DATE_FORMATTER));
            emailVars.put("oldMatchTime", previousTime.format(TIME_FORMATTER));
            emailVars.put("newMatchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("newMatchTime", match.getScheduledTime().format(TIME_FORMATTER));
            emailVars.put("courtNumber", "Court " + match.getCourtNumber());

            // Use match-rescheduled template or fall back to a generic one
            emailService.sendTemplateEmail(
                    recipient.getEmail(),
                    subject,
                    "match-rescheduled",
                    emailVars
            );

            log.info("Sent match reschedule email to: {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send match reschedule email: {}", e.getMessage());
        }
    }

    /**
     * Sends an email notification about a match cancellation
     */
    private void sendMatchCancellationEmail(Match match, User recipient, boolean isPlayer) {
        try {
            String subject = "Match Cancellation - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();

            // Common variables for both player and referee templates
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());
            emailVars.put("matchDate", match.getScheduledTime().format(DATE_FORMATTER));
            emailVars.put("matchTime", match.getScheduledTime().format(TIME_FORMATTER));
            emailVars.put("cancellationDate", LocalDateTime.now().format(DATE_FORMATTER));

            if (isPlayer) {
                // For players
                emailVars.put("playerName", recipient.getFirstName() + " " + recipient.getLastName());

                emailService.sendTemplateEmail(
                        recipient.getEmail(),
                        subject,
                        "match-cancellation-player",
                        emailVars
                );
            } else {
                // For referees
                emailVars.put("refereeName", recipient.getFirstName() + " " + recipient.getLastName());

                emailService.sendTemplateEmail(
                        recipient.getEmail(),
                        subject,
                        "match-cancellation",
                        emailVars
                );
            }

            log.info("Sent match cancellation email to: {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send match cancellation email: {}", e.getMessage());
        }
    }

    private MatchDto mapToDto(Match match) {
        return MatchDto.builder()
                .id(match.getId())
                .tournamentId(match.getTournament().getId())
                .tournamentName(match.getTournament().getName())
                .player1Id(match.getPlayer1().getId())
                .player1Name(match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName())
                .player2Id(match.getPlayer2().getId())
                .player2Name(match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName())
                .refereeId(match.getReferee().getId())
                .refereeName(match.getReferee().getFirstName() + " " + match.getReferee().getLastName())
                .courtNumber(match.getCourtNumber())
                .scheduledTime(match.getScheduledTime())
                .status(match.getStatus())
                .round(match.getRound())
                .build();
    }

    private MatchScoreDto mapScoreToDto(MatchScore score) {
        return MatchScoreDto.builder()
                .id(score.getId())
                .matchId(score.getMatch().getId())
                .setNumber(score.getSetNumber())
                .player1Score(score.getPlayer1Score())
                .player2Score(score.getPlayer2Score())
                .build();
    }

    private String determineWinner(List<MatchScore> scores, Match match) {
        if (match.getStatus() != Match.MatchStatus.COMPLETED || scores.isEmpty()) {
            return "Match not completed";
        }

        int player1Sets = 0;
        int player2Sets = 0;

        for (MatchScore score : scores) {
            if (score.getPlayer1Score() > score.getPlayer2Score()) {
                player1Sets++;
            } else if (score.getPlayer2Score() > score.getPlayer1Score()) {
                player2Sets++;
            }
        }

        if (player1Sets > player2Sets) {
            return match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName();
        } else if (player2Sets > player1Sets) {
            return match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName();
        } else {
            return "Tie";
        }
    }

    /**
     * Validates that a player is registered for a tournament
     *
     * @param player The player to validate
     * @param tournament The tournament to validate against
     * @throws IllegalArgumentException if the player is not registered for the tournament
     */
    private void validatePlayerRegistration(User player, Tournament tournament) {
        if (player.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("User must be a player");
        }

        // Check if player is registered for the tournament
        Optional<TournamentRegistration> registration =
                registrationRepository.findByPlayerAndTournament(player, tournament);

        if (registration.isEmpty()) {
            throw new IllegalArgumentException(
                    "Player " + player.getFirstName() + " " + player.getLastName() +
                            " is not registered for tournament " + tournament.getName());
        }

        // Check if the registration is approved
        if (registration.get().getStatus() != TournamentRegistration.RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Player " + player.getFirstName() + " " + player.getLastName() +
                            " is not approved for tournament " + tournament.getName());
        }
    }

    /**
     * Validates that a match scheduled time is within the tournament dates
     *
     * @param scheduledTime The scheduled time to validate
     * @param tournament The tournament to validate against
     * @throws IllegalArgumentException if the match is scheduled outside tournament dates
     */
    private void validateMatchTiming(LocalDateTime scheduledTime, Tournament tournament) {
        if (scheduledTime == null) {
            throw new IllegalArgumentException("Match must have a scheduled time");
        }

        LocalDateTime tournamentStart = tournament.getStartDate().atStartOfDay();
        LocalDateTime tournamentEnd = tournament.getEndDate().plusDays(1).atStartOfDay();

        if (scheduledTime.isBefore(tournamentStart)) {
            throw new IllegalArgumentException("Match cannot be scheduled before tournament starts");
        }

        if (scheduledTime.isAfter(tournamentEnd)) {
            throw new IllegalArgumentException("Match cannot be scheduled after tournament ends");
        }
    }
}