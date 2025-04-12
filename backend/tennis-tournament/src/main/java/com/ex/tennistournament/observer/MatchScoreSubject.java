package com.ex.tennistournament.observer;

import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import org.springframework.stereotype.Component;

/**
 * Updated MatchScoreSubject to include event type information
 */
@Component
public class MatchScoreSubject extends AbstractSubject {

    public void scoreUpdated(Match match, MatchScore score) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) score.getSetNumber(),
                score.getPlayer1Score(),
                score.getPlayer2Score(),
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_UPDATED"
        );

        notifyObservers("Match score updated", event);
    }

    public void scoreAdded(Match match, MatchScore score) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) score.getSetNumber(),
                score.getPlayer1Score(),
                score.getPlayer2Score(),
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_ADDED"
        );

        notifyObservers("New match score added", event);
    }

    public void scoreDeleted(Match match, Integer setNumber) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                (long) setNumber,
                null,
                null,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_SCORE_DELETED"
        );

        notifyObservers("Match score deleted", event);
    }

    public void matchCompleted(Match match) {
        MatchScoreEvent event = new MatchScoreEvent(
                match.getId(),
                null,
                null,
                null,
                match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName(),
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName(),
                "MATCH_COMPLETED"
        );

        notifyObservers("Match completed", event);
    }
}