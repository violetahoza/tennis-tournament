package com.ex.tennistournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing user notifications.
 * Maps to the 'notifications' table in the database.
 *
 * Features:
 * - User-specific notifications
 * - Read status tracking
 * - Notification type categorization
 * - Automatic timestamp tracking
 *
 * Use cases:
 * - Tournament registration updates
 * - Match schedule notifications
 * - System announcements
 *
 * Constraints:
 * - Message and type are required
 * - Read status defaults to false
 * - Timestamp is automatically set
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private String type;
}