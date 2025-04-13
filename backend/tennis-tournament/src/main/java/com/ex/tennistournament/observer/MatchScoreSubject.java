package com.ex.tennistournament.observer;

import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.repository.MatchScoreRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Subject implementation for broadcasting match score events in the tennis tournament system.
 * Extends AbstractSubject to inherit core Observer pattern functionality.
 *
 * Key responsibilities:
 * - Broadcasts score updates to registered observers
 * - Creates and dispatches MatchScoreEvent objects
 * - Handles different types of match events:
 *   * Score updates
 *   * New score additions
 *   * Score deletions
 *   * Match completions
 */
@Component
public class MatchScoreSubject extends AbstractSubject {

    private final MatchScoreRepository matchScoreRepository;

    public MatchScoreSubject(MatchScoreRepository matchScoreRepository) {
        this.matchScoreRepository = matchScoreRepository;
    }

    /**
     * Notifies observers when a match score is updated.
     * Creates and broadcasts a MATCH_SCORE_UPDATED event with the new score details.
     *
     * @param match The match being updated
     * @param score The new score information
     */
    public void scoreUpdated(Match match, MatchScore score) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) score.getSetNumber(),
                score.getPlayer1Score(),
                score.getPlayer2Score(),
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_UPDATED",
                null // No winner for score updates
        );

        notifyObservers("Match score updated", event);
    }

    /**
     * Notifies observers when a new match score is added.
     * Creates and broadcasts a MATCH_SCORE_ADDED event for the new set score.
     *
     * @param match The match receiving the new score
     * @param score The score being added
     */
    public void scoreAdded(Match match, MatchScore score) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) score.getSetNumber(),
                score.getPlayer1Score(),
                score.getPlayer2Score(),
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_ADDED",
                null // No winner for score additions
        );

        notifyObservers("New match score added", event);
    }

    /**
     * Notifies observers when a match score is deleted.
     * Creates and broadcasts a MATCH_SCORE_DELETED event for the removed set.
     *
     * @param match The match with the deleted score
     * @param setNumber The set number that was deleted
     */
    public void scoreDeleted(Match match, Integer setNumber) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) setNumber,
                null,
                null,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_DELETED",
                null // No winner for score deletions
        );

        notifyObservers("Match score deleted", event);
    }

    /**
     * Notifies observers when a match is completed.
     * Creates and broadcasts a MATCH_COMPLETED event with final match details.
     *
     * @param match The completed match
     */
    public void matchCompleted(Match match) {
        // Get scores from repository instead of match entity
        List<MatchScore> scores = matchScoreRepository.findByMatch(match);

        if (scores == null || scores.isEmpty()) {
            throw new IllegalStateException("Cannot determine winner - no scores available");
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

        if (player1Sets == player2Sets) {
            throw new IllegalStateException("Match cannot be completed with tied scores");
        }

        String winnerName = player1Sets > player2Sets
                ? match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName()
                : match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName();

        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                null,
                null,
                null,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_COMPLETED",
                winnerName
        );

        notifyObservers("Match completed", event);
    }

}