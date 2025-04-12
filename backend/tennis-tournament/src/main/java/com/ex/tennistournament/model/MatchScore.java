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
 * Entity class representing individual set scores for a tennis match.
 * Maps to the 'match_scores' table in the database.
 *
 * Relationships:
 * - Many-to-One with Match (each match can have multiple set scores)
 *
 * Key features:
 * - Tracks scores for each set in a tennis match
 * - Maintains set number for ordering
 * - Records scores for both players
 * - Automatic timestamp management
 *
 * Constraints:
 * - Unique combination of match_id and set_number
 * - Match reference is required
 * - Set number and player scores cannot be null
 * - Timestamps are automatically managed
 */
@Entity
@Table(name = "match_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "set_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "player1_score", nullable = false)
    private Integer player1Score;

    @Column(name = "player2_score", nullable = false)
    private Integer player2Score;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
