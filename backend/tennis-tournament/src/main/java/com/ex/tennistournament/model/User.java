package com.ex.tennistournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing system users with authentication.
 * Maps to the 'users' table in the database.
 * Implements Spring Security's UserDetails interface.
 *
 * User Types:
 * - PLAYER: Regular tournament participant
 * - REFEREE: Match official
 * - ADMIN: System administrator
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Player-specific fields
    @Enumerated(EnumType.STRING)
    @Column(name = "hand_preference")
    private HandPreference handPreference;

    // Referee-specific fields
    @Column(name = "certification_level")
    private String certificationLevel;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /**
     * Enum defining available user roles.
     * Used for role-based access control.
     *
     * Roles hierarchy:
     * ADMIN > REFEREE > PLAYER
     */
    public enum UserType {
        PLAYER, REFEREE, ADMIN
    }

    /**
     * Enum defining player's dominant hand preference.
     */
    public enum HandPreference {
        RIGHT, LEFT
    }

    /**
     * Returns the authorities granted to the user.
     * This implementation returns a single authority based on the user's type.
     *
     * @return a collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add the user type as an authority
        authorities.add(new SimpleGrantedAuthority(userType.name()));

        // Log the authorities being granted for debugging
        log.debug("User {} has authorities: {}", username, authorities);

        return authorities;
    }

    /**
     * Indicates whether the user's account is expired.
     *
     * @return true if the account is not expired, false otherwise
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     *
     * @return true if the account is not locked, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials are expired.
     *
     * @return true if the credentials are not expired, false otherwise
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled.
     *
     * @return true if the user is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}