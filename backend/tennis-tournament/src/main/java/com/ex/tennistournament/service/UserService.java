package com.ex.tennistournament.service;

import com.ex.tennistournament.builder.UserBuilder;
import com.ex.tennistournament.dto.JwtResponseDto;
import com.ex.tennistournament.dto.LoginDto;
import com.ex.tennistournament.dto.PasswordUpdateDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.dto.UserRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing user operations in the tennis tournament system.
 * Provides user authentication, registration, and profile management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Registers a new user in the system.
     *
     * @param registrationDto the user registration details
     * @return the registered user as a DTO
     * @throws IllegalArgumentException if the username or email is already in use
     */
    @Transactional
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        // Check if username is already taken
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        try {
            // Create new user using the Builder pattern
            User user = new UserBuilder(passwordEncoder)
                    .username(registrationDto.getUsername())
                    .password(registrationDto.getPassword())
                    .email(registrationDto.getEmail())
                    .firstName(registrationDto.getFirstName())
                    .lastName(registrationDto.getLastName())
                    .userType(registrationDto.getUserType())
                    .build();

            User savedUser = userRepository.save(user);
            return mapUserToDto(savedUser);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to register user: " + e.getMessage());
        }
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param loginDto the login credentials
     * @return the JWT response containing the token and user details
     */
    public JwtResponseDto authenticateUser(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User userDetails = (User) authentication.getPrincipal();

        return JwtResponseDto.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .userType(userDetails.getUserType())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .build();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user
     * @return the user as a DTO
     * @throws ResourceNotFoundException if the user is not found
     */
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDto(user);
    }

    /**
     * Retrieves all users in the system.
     *
     * @return a list of user DTOs
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all users of a specific type.
     *
     * @param userType the type of users to retrieve
     * @return a list of user DTOs
     */
    public List<UserDto> getUsersByType(User.UserType userType) {
        return userRepository.findByUserType(userType).stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing user's profile.
     *
     * @param id      the ID of the user to update
     * @param userDto the updated user details
     * @return the updated user as a DTO
     * @throws AccessDeniedException if the current user is not authorized to update the profile
     * @throws IllegalArgumentException if the username or email is already in use
     */
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Security check: ensure the current user can only update their own profile
        // unless they are an ADMIN
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getId().equals(id) && currentUser.getUserType() != User.UserType.ADMIN) {
            throw new AccessDeniedException("You can only update your own profile.");
        }

        // Check if username is already taken by another user
        if (!existingUser.getUsername().equals(userDto.getUsername()) &&
                userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Check if email is already in use by another user
        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        try {
            // Use Builder to create updated user
            UserBuilder builder = new UserBuilder(passwordEncoder)
                    .username(userDto.getUsername())
                    .email(userDto.getEmail())
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName());

            // Only administrators can change user roles
            if (currentUser.getUserType() == User.UserType.ADMIN && userDto.getUserType() != null) {
                builder.userType(userDto.getUserType());
            } else {
                builder.userType(existingUser.getUserType());
            }

            // Update password if provided
            String password = userDto.getPassword();
            if (password != null && !password.isEmpty()) {
                builder.password(password);
            }

            User updatedUser = builder.build();

            // Set fields that should be preserved
            updatedUser.setId(existingUser.getId());
            updatedUser.setCreatedAt(existingUser.getCreatedAt());

            // If password wasn't changed, preserve the existing encoded password
            if (password == null || password.isEmpty()) {
                updatedUser.setPassword(existingUser.getPassword());
            }

            // Set role-specific fields
            if (existingUser.getUserType() == User.UserType.PLAYER) {
                updatedUser.setHandPreference(userDto.getHandPreference());
            } else if (existingUser.getUserType() == User.UserType.REFEREE) {
                updatedUser.setCertificationLevel(userDto.getCertificationLevel());
                updatedUser.setYearsOfExperience(userDto.getYearsOfExperience());
            }

            User savedUser = userRepository.save(updatedUser);
            return mapUserToDto(savedUser);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to update user: " + e.getMessage());
        }
    }

    /**
     * Updates a user's password.
     *
     * @param id          the ID of the user
     * @param passwordDto the password update details
     * @return the updated user as a DTO
     * @throws AccessDeniedException if the current user is not authorized to update the password
     * @throws IllegalArgumentException if the current password is incorrect
     */
    @Transactional
    public UserDto updatePassword(Long id, PasswordUpdateDto passwordDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Security check: ensure users can only change their own password
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getId().equals(id)) {
            throw new AccessDeniedException("You can only change your own password.");
        }

        // Verify current password
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), passwordDto.getCurrentPassword())
            );
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        try {
            // Validate the new password using the Builder
            UserBuilder builder = new UserBuilder(passwordEncoder);
            builder.password(passwordDto.getNewPassword());

            // We don't call build() because we only want to validate the password
            // and not create a new User object

            // Update password
            user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
            User updatedUser = userRepository.save(user);

            return mapUserToDto(updatedUser);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Failed to update password: " + e.getMessage());
        }
    }

    /**
     * Deletes a user from the system.
     *
     * @param id the ID of the user to delete
     * @throws IllegalStateException if the user is the last admin or has active matches/registrations
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if this is the last admin
        if (user.getUserType() == User.UserType.ADMIN) {
            long adminCount = userRepository.findByUserType(User.UserType.ADMIN).size();
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last admin user. At least one admin must remain.");
            }
        }

        // Check if user has any matches as referee
        List<Match> refereeMatches = matchRepository.findByReferee(user);
        if (!refereeMatches.isEmpty()) {
            // Check for any active matches
            boolean hasActiveMatches = refereeMatches.stream()
                    .anyMatch(match -> match.getStatus() == Match.MatchStatus.SCHEDULED ||
                            match.getStatus() == Match.MatchStatus.IN_PROGRESS);

            if (hasActiveMatches) {
                throw new IllegalStateException(
                        "Cannot delete referee assigned to ongoing or upcoming matches.");
            }
        }

        // Check if player has any active tournament registrations
        if (user.getUserType() == User.UserType.PLAYER) {
            List<TournamentRegistration> registrations = registrationRepository.findByPlayer(user);
            boolean hasActiveRegistrations = registrations.stream()
                    .anyMatch(reg -> reg.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED);

            if (hasActiveRegistrations) {
                throw new IllegalStateException(
                        "Cannot delete player registered in ongoing tournaments.");
            }
        }

        userRepository.deleteById(id);
    }

    /**
     * Maps a User entity to a UserDto.
     *
     * @param user the User entity
     * @return the UserDto
     */
    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .handPreference(user.getHandPreference())
                .certificationLevel(user.getCertificationLevel())
                .yearsOfExperience(user.getYearsOfExperience())
                // Do not include password in the response
                .build();
    }
}