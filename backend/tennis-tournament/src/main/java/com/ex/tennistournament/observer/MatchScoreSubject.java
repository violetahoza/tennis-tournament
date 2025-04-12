package com.ex.tennistournament.observer;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import org.springframework.stereotype.Component;

/**
 * MatchScoreSubject is a concrete subject that tracks match score updates.
 * It extends the AbstractSubject which provides the basic observer pattern implementation.
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
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
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
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
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
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
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
                match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName()
        );

        notifyObservers("Match completed", event);
    }
}