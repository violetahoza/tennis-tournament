package com.ex.tennistournament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for tennis match score data.
 * Represents the score for a specific set in a tennis match.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long userId;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;
    private String type;
}