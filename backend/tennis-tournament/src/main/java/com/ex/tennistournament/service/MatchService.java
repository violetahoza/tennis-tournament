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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public List<MatchDto> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return matchRepository.findByTournament(tournament).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return matchRepository.findByPlayer1OrPlayer2(player, player).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByReferee(Long refereeId) {
        User referee = userRepository.findById(refereeId)
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + refereeId));

        return matchRepository.findByReferee(referee).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MatchDto getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return mapToDto(match);
    }

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

            // Send notifications to players
            sendMatchCreatedNotifications(savedMatch);

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

        // Validate that players are registered for the tournament
        validatePlayerRegistration(player1, tournament);
        validatePlayerRegistration(player2, tournament);

        // Validate match timing
        validateMatchTiming(matchDto.getScheduledTime(), tournament);

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
            if (existingMatch.getStatus() != savedMatch.getStatus()) {
                sendStatusChangeNotification(savedMatch, existingMatch.getStatus());
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
        // For in-progress matches, only allow updating the status, court number, and referee
        if (existingMatch.getStatus() == Match.MatchStatus.IN_PROGRESS) {
            // Can only update to COMPLETED or CANCELLED
            if (matchDto.getStatus() != Match.MatchStatus.IN_PROGRESS &&
                    matchDto.getStatus() != Match.MatchStatus.COMPLETED &&
                    matchDto.getStatus() != Match.MatchStatus.CANCELLED) {
                throw new IllegalArgumentException("In-progress match can only be updated to COMPLETED or CANCELLED");
            }

            // Allow changing referee
            if (!existingMatch.getReferee().getId().equals(matchDto.getRefereeId())) {
                User newReferee = userRepository.findById(matchDto.getRefereeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

                if (newReferee.getUserType() != User.UserType.REFEREE) {
                    throw new IllegalArgumentException("Referee must be a referee");
                }

                existingMatch.setReferee(newReferee);
            }

            // Allow changing court number
            if (matchDto.getCourtNumber() != null) {
                existingMatch.setCourtNumber(matchDto.getCourtNumber());
            }

            existingMatch.setStatus(matchDto.getStatus());
        } else if (existingMatch.getStatus() == Match.MatchStatus.COMPLETED) {
            // For completed matches, only allow changing to CANCELLED
            if (matchDto.getStatus() == Match.MatchStatus.CANCELLED) {
                existingMatch.setStatus(Match.MatchStatus.CANCELLED);
            } else if (matchDto.getStatus() != Match.MatchStatus.COMPLETED) {
                throw new IllegalArgumentException("Completed match can only be updated to CANCELLED");
            }
        }

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
            sendMatchCancelledNotification(match);
        }

        matchRepository.deleteById(id);
    }

    private void sendMatchCreatedNotifications(Match match) {
        String message = "New match scheduled against " +
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName() +
                " at " + match.getScheduledTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));

        // Player 1 notification
        NotificationDto notification1 = new NotificationDto();
        notification1.setUserId(match.getPlayer1().getId());
        notification1.setType("MATCH_SCHEDULED");
        notification1.setMessage(message);
        notification1.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification1);

        // Player 2 notification
        NotificationDto notification2 = new NotificationDto();
        notification2.setUserId(match.getPlayer2().getId());
        notification2.setType("MATCH_SCHEDULED");
        notification2.setMessage("New match scheduled against " +
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName() +
                " at " + match.getScheduledTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        notification2.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification2);
    }

    private void sendStatusChangeNotification(Match match, Match.MatchStatus previousStatus) {
        String message = "Match status changed from " + previousStatus + " to " + match.getStatus();

        // Player 1 notification
        NotificationDto notification1 = new NotificationDto();
        notification1.setUserId(match.getPlayer1().getId());
        notification1.setType("MATCH_STATUS_CHANGE");
        notification1.setMessage(message);
        notification1.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification1);

        // Player 2 notification
        NotificationDto notification2 = new NotificationDto();
        notification2.setUserId(match.getPlayer2().getId());
        notification2.setType("MATCH_STATUS_CHANGE");
        notification2.setMessage(message);
        notification2.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification2);
    }

    private void sendMatchCancelledNotification(Match match) {
        String message = "Your match scheduled for " +
                match.getScheduledTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) +
                " has been cancelled";

        // Player 1 notification
        NotificationDto notification1 = new NotificationDto();
        notification1.setUserId(match.getPlayer1().getId());
        notification1.setType("MATCH_CANCELLED");
        notification1.setMessage(message);
        notification1.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification1);

        // Player 2 notification
        NotificationDto notification2 = new NotificationDto();
        notification2.setUserId(match.getPlayer2().getId());
        notification2.setType("MATCH_CANCELLED");
        notification2.setMessage(message);
        notification2.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(notification2);
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
}