package com.ex.tennistournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class representing a tennis match in the tournament system.
 * Maps to the 'matches' table in the database.
 *
 * Relationships:
 * - Many-to-One with Tournament
 * - Many-to-One with User (player1, player2, referee)
 *
 * Key features:
 * - Unique identifier for each match
 * - References to both players and referee
 * - Court assignment and scheduling information
 * - Match status tracking (SCHEDULED, IN_PROGRESS, etc.)
 * - Tournament round tracking (ROUND_1, QUARTER_FINAL, etc.)
 * - Automatic timestamp management
 *
 * Constraints:
 * - Tournament, players, and referee are required
 * - Scheduled time is mandatory
 * - Status must be one of predefined enum values
 * - Court number is optional
 */
@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    @ManyToOne
    @JoinColumn(name = "referee_id", nullable = false)
    private User referee;

    @Column(name = "court_number")
    private Integer courtNumber;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    private Round round;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum defining possible match statuses.
     * Used to track the progression of a match.
     */
    public enum MatchStatus {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    /**
     * Enum defining tournament rounds.
     * Represents the stage of the tournament.
     */
    public enum Round {
        ROUND_1, ROUND_2, QUARTER_FINAL, SEMI_FINAL, FINAL
    }
}