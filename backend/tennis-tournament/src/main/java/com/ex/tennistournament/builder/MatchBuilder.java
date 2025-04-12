package com.ex.tennistournament.builder;

import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.User;

import java.time.LocalDateTime;

/**
 * MatchBuilder is a concrete builder for creating Match objects.
 * It implements the Builder interface and provides methods for setting Match attributes.
 */
public class MatchBuilder implements Builder<Match> {
    private final Match match;

    public MatchBuilder() {
        this.match = new Match();
        // Set default values
        this.match.setStatus(Match.MatchStatus.SCHEDULED);
    }

    public MatchBuilder tournament(Tournament tournament) {
        this.match.setTournament(tournament);
        return this;
    }

    public MatchBuilder player1(User player1) {
        this.match.setPlayer1(player1);
        return this;
    }

    public MatchBuilder player2(User player2) {
        this.match.setPlayer2(player2);
        return this;
    }

    public MatchBuilder referee(User referee) {
        this.match.setReferee(referee);
        return this;
    }

    public MatchBuilder courtNumber(Integer courtNumber) {
        this.match.setCourtNumber(courtNumber);
        return this;
    }

    public MatchBuilder scheduledTime(LocalDateTime scheduledTime) {
        this.match.setScheduledTime(scheduledTime);
        return this;
    }

    public MatchBuilder status(Match.MatchStatus status) {
        this.match.setStatus(status);
        return this;
    }

    public MatchBuilder round(Match.Round round) {
        this.match.setRound(round);
        return this;
    }

    @Override
    public Match build() {
        // Validate the match before returning it
        validateMatch();
        return match;
    }

    private void validateMatch() {
        if (match.getTournament() == null) {
            throw new IllegalStateException("Match must have a tournament");
        }
        if (match.getPlayer1() == null) {
            throw new IllegalStateException("Match must have player 1");
        }
        if (match.getPlayer2() == null) {
            throw new IllegalStateException("Match must have player 2");
        }
        if (match.getReferee() == null) {
            throw new IllegalStateException("Match must have a referee");
        }
        if (match.getScheduledTime() == null) {
            throw new IllegalStateException("Match must have a scheduled time");
        }
        if (match.getStatus() == null) {
            throw new IllegalStateException("Match must have a status");
        }
        if (match.getRound() == null) {
            throw new IllegalStateException("Match must have a round");
        }

        if (match.getPlayer1().equals(match.getPlayer2())) {
            throw new IllegalStateException("Player 1 and Player 2 cannot be the same");
        }

        if (match.getPlayer1().getUserType() != User.UserType.PLAYER) {
            throw new IllegalStateException("Player 1 must be a player");
        }

        if (match.getPlayer2().getUserType() != User.UserType.PLAYER) {
            throw new IllegalStateException("Player 2 must be a player");
        }

        if (match.getReferee().getUserType() != User.UserType.REFEREE) {
            throw new IllegalStateException("Referee must be a referee");
        }
    }
}