package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.observer.MatchScoreLogger;
import com.ex.tennistournament.observer.MatchScoreSubject;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing tennis match scores and game progression.
 * Handles creation, updates, and completion of match scores while enforcing tennis rules.
 */
@Service
@RequiredArgsConstructor
public class MatchScoreService {

    private final MatchScoreRepository matchScoreRepository;
    private final MatchRepository matchRepository;
    private final MatchScoreSubject matchScoreSubject;
    private final MatchScoreLogger matchScoreLogger;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public List<MatchScoreDto> getScoresByMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        return matchScoreRepository.findByMatchOrderBySetNumber(match).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MatchScoreDto getScoreById(Long id) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));
        return mapToDto(score);
    }

    @Transactional
    public MatchScoreDto createScore(MatchScoreDto scoreDto) {
        Match match = matchRepository.findById(scoreDto.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + scoreDto.getMatchId()));

        // Verify match is in progress or scheduled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add scores to completed or cancelled matches");
        }

        // Verify current user is the referee of the match or an admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Authentication required");
        }

        User currentUser = (User) authentication.getPrincipal();

        boolean isReferee = match.getReferee() != null &&
                match.getReferee().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getUserType() == User.UserType.ADMIN;

        if (!isReferee && !isAdmin) {
            throw new IllegalStateException("Only the assigned referee or an admin can update match scores");
        }

        // Check if set number already exists
        matchScoreRepository.findByMatchOrderBySetNumber(match).stream()
                .filter(existingScore -> existingScore.getSetNumber().equals(scoreDto.getSetNumber()))
                .findFirst()
                .ifPresent(existingScore -> {
                    throw new IllegalArgumentException("Score for set " + scoreDto.getSetNumber() + " already exists");
                });

        // Validate set scores based on tennis rules
        validateTennisSetScore(scoreDto.getPlayer1Score(), scoreDto.getPlayer2Score());

        // Create new score
        MatchScore score = MatchScore.builder()
                .match(match)
                .setNumber(scoreDto.getSetNumber())
                .player1Score(scoreDto.getPlayer1Score())
                .player2Score(scoreDto.getPlayer2Score())
                .build();

        // Update match status to IN_PROGRESS if it was SCHEDULED
        if (match.getStatus() == Match.MatchStatus.SCHEDULED) {
            match.setStatus(Match.MatchStatus.IN_PROGRESS);
            matchRepository.save(match);
        }

        MatchScore savedScore = matchScoreRepository.save(score);

        // Notify observers about the new score
        matchScoreSubject.scoreAdded(match, savedScore);

        return mapToDto(savedScore);
    }

    @Transactional
    public MatchScoreDto updateScore(Long id, MatchScoreDto scoreDto) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));

        Match match = score.getMatch();

        // Verify match is not completed or cancelled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update scores of completed or cancelled matches");
        }

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can update match scores");
        }

        // Validate set scores based on tennis rules
        validateTennisSetScore(scoreDto.getPlayer1Score(), scoreDto.getPlayer2Score());

        score.setPlayer1Score(scoreDto.getPlayer1Score());
        score.setPlayer2Score(scoreDto.getPlayer2Score());

        MatchScore updatedScore = matchScoreRepository.save(score);

        // Notify observers about the updated score
        matchScoreSubject.scoreUpdated(match, updatedScore);

//        // Send notifications to players about the updated score
//        sendScoreNotifications(match, updatedScore, "Score updated for set " + updatedScore.getSetNumber() +
//                ": " + updatedScore.getPlayer1Score() + "-" + updatedScore.getPlayer2Score());

        return mapToDto(updatedScore);
    }

    @Transactional
    public void deleteScore(Long id) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));

        Match match = score.getMatch();
        int setNumber = score.getSetNumber();

        // Verify match is not completed or cancelled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot delete scores of completed or cancelled matches");
        }

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can delete match scores");
        }

        matchScoreRepository.deleteById(id);

        // Notify observers about the deleted score
        matchScoreSubject.scoreDeleted(match, setNumber);

//        // Send notifications to players and admin
//        sendScoreNotifications(match, null, "Score deleted for set " + setNumber);
    }

    @Transactional
    public void completeMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can complete the match");
        }

        // Check if there are any scores recorded
        List<MatchScore> scores = matchScoreRepository.findByMatch(match);
        if (scores.isEmpty()) {
            throw new IllegalStateException("Cannot complete match without any scores");
        }

        // Determine if there's a clear winner
        int player1Sets = 0;
        int player2Sets = 0;

        for (MatchScore score : scores) {
            if (score.getPlayer1Score() > score.getPlayer2Score()) {
                player1Sets++;
            } else if (score.getPlayer2Score() > score.getPlayer1Score()) {
                player2Sets++;
            }
        }

        // Check if we have a winner
        if (player1Sets == player2Sets) {
            throw new IllegalStateException("Cannot complete the match with tied scores. There must be a winner.");
        }

        // Update match status to COMPLETED
        match.setStatus(Match.MatchStatus.COMPLETED);
        matchRepository.save(match);

        // Notify observers about the completed match
        matchScoreSubject.matchCompleted(match);

        // Determine winner name
        String winnerName = player1Sets > player2Sets ?
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName() :
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName();

//        // Send notifications to players about the completed match
//        sendMatchCompletionNotifications(match, winnerName);

        // Send notification to admins about the completed match
        sendMatchCompletionNotificationToAdmins(match, winnerName);
    }

    /**
     * Send notifications to players about score updates
     */
    private void sendScoreNotifications(Match match, MatchScore score, String message) {
        // Send to player 1
        sendNotification(match.getPlayer1().getId(), "MATCH_SCORE", message);

        // Send to player 2
        sendNotification(match.getPlayer2().getId(), "MATCH_SCORE", message);
    }

    /**
     * Send match completion notifications to players and referee
     */
    private void sendMatchCompletionNotifications(Match match, String winnerName) {
        String message = "Match completed! Winner: " + winnerName;

        // Send to player 1
        sendNotification(match.getPlayer1().getId(), "MATCH_COMPLETED", message);

        // Send to player 2
        sendNotification(match.getPlayer2().getId(), "MATCH_COMPLETED", message);

        // Send to referee
        sendNotification(match.getReferee().getId(), "MATCH_COMPLETED", message);
    }

    /**
     * Send match completion notifications to all admin users
     */
    private void sendMatchCompletionNotificationToAdmins(Match match, String winnerName) {
        // Find all admin users
        List<User> admins = userRepository.findByUserType(User.UserType.ADMIN);

        String tournamentName = match.getTournament().getName();
        String message = String.format(
                "Match completed in tournament '%s'. %s vs %s. Winner: %s",
                tournamentName,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                winnerName
        );

        // Send notification to each admin
        for (User admin : admins) {
            sendNotification(admin.getId(), "MATCH_COMPLETED", message);
        }
    }

    /**
     * Send a notification to a specific user
     */
    private void sendNotification(Long userId, String type, String message) {
        NotificationDto notification = NotificationDto.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.sendNotification(notification);
    }

    private MatchScoreDto mapToDto(MatchScore score) {
        return MatchScoreDto.builder()
                .id(score.getId())
                .matchId(score.getMatch().getId())
                .setNumber(score.getSetNumber())
                .player1Score(score.getPlayer1Score())
                .player2Score(score.getPlayer2Score())
                .build();
    }

    /**
     * Validates that a tennis set score follows the rules of tennis.
     *
     * @param player1Score Score of player 1
     * @param player2Score Score of player 2
     * @throws IllegalArgumentException if the score is invalid according to tennis rules
     */
    private void validateTennisSetScore(int player1Score, int player2Score) {
        if (player1Score < 0 || player2Score < 0) {
            throw new IllegalArgumentException("Games cannot be negative");
        }

        // A set cannot end in a tie in tennis
        if (player1Score == player2Score) {
            throw new IllegalArgumentException("Sets cannot end in a tie in tennis");
        }

        if (player1Score > 7 || player2Score > 7) {
            throw new IllegalArgumentException("Maximum game score in a set is 7");
        }

        // Case 1: One player has 6 games
        if ((player1Score == 6 && player2Score < 5) ||
                (player2Score == 6 && player1Score < 5)) {
            // This is a valid score (6-0, 6-1, 6-2, 6-3, 6-4)
            return;
        }

        // Case 2: 7-5 score
        if ((player1Score == 7 && player2Score == 5) ||
                (player2Score == 7 && player1Score == 5)) {
            // This is a valid score (7-5)
            return;
        }

        // Case 3: 7-6 score (tiebreak)
        if ((player1Score == 7 && player2Score == 6) ||
                (player2Score == 7 && player1Score == 6)) {
            // This is a valid score (7-6)
            return;
        }

        // If we reach here, the score is invalid
        throw new IllegalArgumentException("Invalid tennis set score. Valid scores include 6-0 through 6-4, 7-5, and 7-6.");
    }
}