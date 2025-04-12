package com.ex.tennistournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing tournament registrations.
 * Maps to the 'tournament_registrations' table in the database.
 *
 * Relationships:
 * - Many-to-One with User (as player)
 * - Many-to-One with Tournament
 *
 * Key features:
 * - Tracks player registration status for tournaments
 * - Enforces unique player-tournament combinations
 * - Registration status lifecycle management
 * - Timestamps for registration tracking
 *
 * Constraints:
 * - Player and tournament references are required
 * - Status must be one of: PENDING, APPROVED, REJECTED, WAITLISTED
 * - Unique constraint on player_id and tournament_id combination
 */
@Entity
@Table(name = "tournament_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "tournament_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    public enum RegistrationStatus {
        PENDING, APPROVED, REJECTED, WAITLISTED
    }
}