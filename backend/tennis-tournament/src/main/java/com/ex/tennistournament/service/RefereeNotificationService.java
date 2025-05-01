package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing referee notifications.
 * Handles both in-app and email notifications for referee match assignments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefereeNotificationService {

    private final NotificationService notificationService;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Notifies a referee about a new match assignment.
     *
     * @param match The match the referee has been assigned to
     * @param isNewAssignment Whether this is a new assignment or an update
     */
    public void notifyRefereeOfAssignment(Match match, boolean isNewAssignment) {
        User referee = match.getReferee();

        if (referee == null) {
            log.warn("Cannot notify referee for match ID {} - referee is null", match.getId());
            return;
        }

        log.info("Sending match assignment notification to referee: {}", referee.getUsername());

        // Prepare the message
        String formattedDateTime = match.getScheduledTime()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));

        String notificationType = isNewAssignment ? "MATCH_ASSIGNMENT" : "MATCH_ASSIGNMENT_UPDATE";
        String action = isNewAssignment ? "assigned to" : "updated for";

        String message = String.format(
                "You have been %s a match: %s vs %s at %s on court %d",
                action,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                formattedDateTime,
                match.getCourtNumber()
        );

        // Send in-app notification
        NotificationDto notification = NotificationDto.builder()
                .userId(referee.getId())
                .type(notificationType)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        // Send email notification
        sendRefereeAssignmentEmail(match, referee, isNewAssignment);
    }

    /**
     * Sends an email notification to a referee about a match assignment.
     *
     * @param match The match the referee has been assigned to
     * @param referee The referee to notify
     * @param isNewAssignment Whether this is a new assignment or an update
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

            // Format date and time separately for better display
            LocalDateTime matchDateTime = match.getScheduledTime();
            emailVars.put("matchDate", matchDateTime.format(DATE_FORMATTER));
            emailVars.put("matchTime", matchDateTime.format(TIME_FORMATTER));

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
     * Notifies a referee about a match cancellation.
     *
     * @param match The cancelled match
     */
    public void notifyRefereeOfCancellation(Match match) {
        User referee = match.getReferee();

        if (referee == null) {
            log.warn("Cannot notify referee of cancellation for match ID {} - referee is null", match.getId());
            return;
        }

        log.info("Sending match cancellation notification to referee: {}", referee.getUsername());

        // Prepare the message
        String message = String.format(
                "Match you were assigned to referee has been cancelled: %s vs %s",
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
        );

        // Send in-app notification
        NotificationDto notification = NotificationDto.builder()
                .userId(referee.getId())
                .type("MATCH_CANCELLED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        // Send email notification
        sendRefereeCancellationEmail(match, referee);
    }

    /**
     * Sends an email notification to a referee about a match cancellation.
     *
     * @param match The cancelled match
     * @param referee The referee to notify
     */
    private void sendRefereeCancellationEmail(Match match, User referee) {
        try {
            String subject = "Match Cancellation - Tennis Tournament";

            Map<String, Object> emailVars = new HashMap<>();
            emailVars.put("refereeName", referee.getFirstName() + " " + referee.getLastName());
            emailVars.put("tournamentName", match.getTournament().getName());
            emailVars.put("player1Name", match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName());
            emailVars.put("player2Name", match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName());

            // Format date and time
            LocalDateTime matchDateTime = match.getScheduledTime();
            emailVars.put("matchDate", matchDateTime.format(DATE_FORMATTER));
            emailVars.put("matchTime", matchDateTime.format(TIME_FORMATTER));

            emailVars.put("cancellationDate", LocalDateTime.now().format(DATE_FORMATTER));

            emailService.sendTemplateEmail(
                    referee.getEmail(),
                    subject,
                    "match-cancellation", // You'll need to create this template
                    emailVars
            );

            log.info("Sent match cancellation email to referee: {}", referee.getEmail());
        } catch (Exception e) {
            log.error("Failed to send match cancellation email: {}", e.getMessage());
        }
    }
}