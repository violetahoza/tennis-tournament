package com.ex.tennistournament.builder;

import com.ex.tennistournament.model.Tournament;
import java.time.LocalDate;

/**
 * TournamentBuilder is a concrete builder for creating Tournament objects.
 * It implements the Builder interface and provides methods for setting Tournament attributes.
 */
public class TournamentBuilder implements Builder<Tournament> {
    private final Tournament tournament;

    /**
     * Constructor initializes a new Tournament object.
     */
    public TournamentBuilder() {
        this.tournament = new Tournament();
    }

    /**
     * Sets the name of the tournament.
     *
     * @param name the name of the tournament
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder name(String name) {
        this.tournament.setName(name);
        return this;
    }

    /**
     * Sets the description of the tournament.
     *
     * @param description the description of the tournament
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder description(String description) {
        this.tournament.setDescription(description);
        return this;
    }

    /**
     * Sets the location of the tournament.
     *
     * @param location the location of the tournament
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder location(String location) {
        this.tournament.setLocation(location);
        return this;
    }

    /**
     * Sets the start date of the tournament.
     *
     * @param startDate the start date of the tournament
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder startDate(LocalDate startDate) {
        this.tournament.setStartDate(startDate);
        return this;
    }

    /**
     * Sets the end date of the tournament.
     *
     * @param endDate the end date of the tournament
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder endDate(LocalDate endDate) {
        this.tournament.setEndDate(endDate);
        return this;
    }

    /**
     * Sets the registration deadline for the tournament.
     *
     * @param registrationDeadline the registration deadline
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder registrationDeadline(LocalDate registrationDeadline) {
        this.tournament.setRegistrationDeadline(registrationDeadline);
        return this;
    }

    /**
     * Sets the maximum number of participants for the tournament.
     *
     * @param maxParticipants the maximum number of participants
     * @return the current instance of TournamentBuilder
     */
    public TournamentBuilder maxParticipants(Integer maxParticipants) {
        this.tournament.setMaxParticipants(maxParticipants);
        return this;
    }

    /**
     * Builds and returns the Tournament object.
     * Validates the tournament attributes before returning the object.
     *
     * @return the built Tournament object
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
    @Override
    public Tournament build() {
        // Validate the tournament before returning it
        validateTournament();
        return tournament;
    }

    /**
     * Validates the Tournament object to ensure all required attributes are set and valid.
     *
     * @throws IllegalStateException if any required attribute is missing or invalid
     */
    private void validateTournament() {
        if (tournament.getName() == null || tournament.getName().trim().isEmpty()) {
            throw new IllegalStateException("Tournament must have a name");
        }

        if (tournament.getLocation() == null || tournament.getLocation().trim().isEmpty()) {
            throw new IllegalStateException("Tournament must have a location");
        }

        if (tournament.getStartDate() == null) {
            throw new IllegalStateException("Tournament must have a start date");
        }

        if (tournament.getEndDate() == null) {
            throw new IllegalStateException("Tournament must have an end date");
        }

        if (tournament.getRegistrationDeadline() == null) {
            throw new IllegalStateException("Tournament must have a registration deadline");
        }

        if (tournament.getMaxParticipants() == null || tournament.getMaxParticipants() < 2) {
            throw new IllegalStateException("Tournament must have at least 2 participants");
        }

        LocalDate today = LocalDate.now();

        if (tournament.getStartDate().isBefore(today)) {
            throw new IllegalStateException("Tournament start date must be in the future");
        }

        if (tournament.getEndDate().isBefore(tournament.getStartDate())) {
            throw new IllegalStateException("Tournament end date must be after start date");
        }

        if (tournament.getRegistrationDeadline().isAfter(tournament.getStartDate())) {
            throw new IllegalStateException("Registration deadline must be before tournament start date");
        }
    }
}