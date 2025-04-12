package com.ex.tennistournament.builder;

import com.ex.tennistournament.model.Tournament;
import java.time.LocalDate;

/**
 * TournamentBuilder is a concrete builder for creating Tournament objects.
 * It implements the Builder interface and provides methods for setting Tournament attributes.
 */
public class TournamentBuilder implements Builder<Tournament> {
    private final Tournament tournament;

    public TournamentBuilder() {
        this.tournament = new Tournament();
    }

    public TournamentBuilder name(String name) {
        this.tournament.setName(name);
        return this;
    }

    public TournamentBuilder description(String description) {
        this.tournament.setDescription(description);
        return this;
    }

    public TournamentBuilder location(String location) {
        this.tournament.setLocation(location);
        return this;
    }

    public TournamentBuilder startDate(LocalDate startDate) {
        this.tournament.setStartDate(startDate);
        return this;
    }

    public TournamentBuilder endDate(LocalDate endDate) {
        this.tournament.setEndDate(endDate);
        return this;
    }

    public TournamentBuilder registrationDeadline(LocalDate registrationDeadline) {
        this.tournament.setRegistrationDeadline(registrationDeadline);
        return this;
    }

    public TournamentBuilder maxParticipants(Integer maxParticipants) {
        this.tournament.setMaxParticipants(maxParticipants);
        return this;
    }

    @Override
    public Tournament build() {
        // Validate the tournament before returning it
        validateTournament();
        return tournament;
    }

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