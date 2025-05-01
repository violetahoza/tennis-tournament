package com.ex.tennistournament.dto;

import com.ex.tennistournament.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user lookup and basic user information.
 * Used for displaying user details without sensitive information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    // Only used when sending from client to server, never included in responses
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private User.UserType userType;

    // Player-specific fields
    private User.HandPreference handPreference;

    // Additional fields for tournament-related filtering
    private String tournamentStatus;
    private String tournamentName;
    private Long tournamentId;

    // Referee-specific fields
    private String certificationLevel;
    private Integer yearsOfExperience;
}