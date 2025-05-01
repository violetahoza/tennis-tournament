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

    /**
     * Constructor initializes a new Match object with default values.
     */
    public MatchBuilder() {
        this.match = new Match();
        // Set default values
        this.match.setStatus(Match.MatchStatus.SCHEDULED);
    }

    /**
     * Sets the tournament for the match.
     *
     * @param tournament the tournament to set
     * @return the MatchBuilder instance
     */
    public MatchBuilder tournament(Tournament tournament) {
        this.match.setTournament(tournament);
        return this;
    }

    /**
     * Sets player 1 for the match.
     *
     * @param player1 the first player
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder player1(User player1) {
        this.match.setPlayer1(player1);
        return this;
    }

    /**
     * Sets player 2 for the match.
     *
     * @param player2 the second player
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder player2(User player2) {
        this.match.setPlayer2(player2);
        return this;
    }

    /**
     * Sets the referee for the match.
     *
     * @param referee the referee
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder referee(User referee) {
        this.match.setReferee(referee);
        return this;
    }

    /**
     * Sets the court number for the match.
     *
     * @param courtNumber the court number
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder courtNumber(Integer courtNumber) {
        this.match.setCourtNumber(courtNumber);
        return this;
    }

    /**
     * Sets the scheduled time for the match.
     *
     * @param scheduledTime the scheduled time
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder scheduledTime(LocalDateTime scheduledTime) {
        this.match.setScheduledTime(scheduledTime);
        return this;
    }

    /**
     * Sets the status of the match.
     *
     * @param status the match status
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder status(Match.MatchStatus status) {
        this.match.setStatus(status);
        return this;
    }

    /**
     * Sets the round of the match.
     *
     * @param round the match round
     * @return the current instance of MatchBuilder
     */
    public MatchBuilder round(Match.Round round) {
        this.match.setRound(round);
        return this;
    }

    /**
     * Builds and returns the Match object.
     * Validates the match attributes before returning the object.
     *
     * @return the built Match object
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
    @Override
    public Match build() {
        // Validate the match before returning it
        validateMatch();
        return match;
    }

    /**
     * Validates the Match object to ensure all required attributes are set and valid.
     *
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
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