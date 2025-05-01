package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.JwtResponseDto;
import com.ex.tennistournament.dto.LoginDto;
import com.ex.tennistournament.dto.PasswordUpdateDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.dto.UserRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link UserService} class.
 * This class uses Mockito to mock dependencies and test the behavior of the UserService.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private TournamentRegistrationRepository registrationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    @InjectMocks
    private UserService userService;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User adminUser;
    private User playerUser;
    private User refereeUser;
    private UserRegistrationDto registrationDto;
    private LoginDto loginDto;
    private PasswordUpdateDto passwordUpdateDto;

    /**
     * Sets up the test environment before each test.
     * Initializes test data and mocks.
     */
    @BeforeEach
    void setUp() {
        // Set up admin user
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPassword("encoded_admin_password");
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setUserType(User.UserType.ADMIN);

        // Set up player user
        playerUser = new User();
        playerUser.setId(2L);
        playerUser.setUsername("player");
        playerUser.setPassword("encoded_player_password");
        playerUser.setEmail("player@example.com");
        playerUser.setFirstName("Player");
        playerUser.setLastName("User");
        playerUser.setUserType(User.UserType.PLAYER);
        playerUser.setHandPreference(User.HandPreference.RIGHT);

        // Set up referee user
        refereeUser = new User();
        refereeUser.setId(3L);
        refereeUser.setUsername("referee");
        refereeUser.setPassword("encoded_referee_password");
        refereeUser.setEmail("referee@example.com");
        refereeUser.setFirstName("Referee");
        refereeUser.setLastName("User");
        refereeUser.setUserType(User.UserType.REFEREE);
        refereeUser.setCertificationLevel("Advanced");
        refereeUser.setYearsOfExperience(5);

        // Set up registration DTO
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setPassword("Password123"); // Updated to meet password requirements
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");
        registrationDto.setUserType(User.UserType.PLAYER);

        // Set up login DTO
        loginDto = new LoginDto();
        loginDto.setUsername("player");
        loginDto.setPassword("player_password");

        // Set up password update DTO
        passwordUpdateDto = new PasswordUpdateDto();
        passwordUpdateDto.setCurrentPassword("current_password");
        passwordUpdateDto.setNewPassword("New_password123"); // Updated to meet password requirements
    }

    /**
     * Tests successful registration of a new user.
     */
    @Test
    @DisplayName("Should register a new user successfully")
    void registerUser_Success() {
        // Arrange
        registrationDto.setPassword("Password123"); // Updated to meet password requirements

        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encoded_password");

        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setUsername(registrationDto.getUsername());
        savedUser.setPassword("encoded_password");
        savedUser.setEmail(registrationDto.getEmail());
        savedUser.setFirstName(registrationDto.getFirstName());
        savedUser.setLastName(registrationDto.getLastName());
        savedUser.setUserType(registrationDto.getUserType());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserDto result = userService.registerUser(registrationDto);

        // Assert
        assertNotNull(result);
        assertEquals(registrationDto.getUsername(), result.getUsername());
        assertEquals(registrationDto.getEmail(), result.getEmail());
        assertEquals(registrationDto.getFirstName(), result.getFirstName());
        assertEquals(registrationDto.getLastName(), result.getLastName());
        assertEquals(registrationDto.getUserType(), result.getUserType());

        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(passwordEncoder).encode(registrationDto.getPassword());
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(registrationDto.getUsername(), capturedUser.getUsername());
        assertEquals("encoded_password", capturedUser.getPassword());
        assertEquals(registrationDto.getEmail(), capturedUser.getEmail());
    }

    /**
     * Tests that an exception is thrown when the username is already taken.
     */
    @Test
    @DisplayName("Should throw exception when username is already taken")
    void registerUser_UsernameAlreadyTaken() {
        // Arrange
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(registrationDto)
        );

        assertEquals("Username is already taken", exception.getMessage());

        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that an exception is thrown when the email is already in use.
     */
    @Test
    @DisplayName("Should throw exception when email is already in use")
    void registerUser_EmailAlreadyInUse() {
        // Arrange
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(registrationDto)
        );

        assertEquals("Email is already in use", exception.getMessage());

        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful authentication of a user and JWT generation.
     */
    @Test
    @DisplayName("Should authenticate user and return JWT")
    void authenticateUser_Success() {
        // Arrange
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        when(authenticationManager.authenticate(argThat(token ->
                token.getPrincipal().equals(loginDto.getUsername()) &&
                        token.getCredentials().equals(loginDto.getPassword()))))
                .thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(playerUser);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt_token");

        // Act
        JwtResponseDto result = userService.authenticateUser(loginDto);

        // Assert
        assertNotNull(result);
        assertEquals("jwt_token", result.getToken());
        assertEquals("Bearer", result.getType());
        assertEquals(playerUser.getId(), result.getId());
        assertEquals(playerUser.getUsername(), result.getUsername());
        assertEquals(playerUser.getEmail(), result.getEmail());
        assertEquals(playerUser.getUserType(), result.getUserType());
        assertEquals(playerUser.getFirstName(), result.getFirstName());
        assertEquals(playerUser.getLastName(), result.getLastName());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(authentication);
    }

    /**
     * Tests that an exception is thrown when authentication fails.
     */
    @Test
    @DisplayName("Should throw exception when authentication fails")
    void authenticateUser_AuthenticationFails() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () ->
                userService.authenticateUser(loginDto)
        );

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtils);
    }

    /**
     * Tests retrieving a user by ID successfully.
     */
    @Test
    @DisplayName("Should return user by ID")
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));

        // Act
        UserDto result = userService.getUserById(playerUser.getId());

        // Assert
        assertNotNull(result);
        assertEquals(playerUser.getId(), result.getId());
        assertEquals(playerUser.getUsername(), result.getUsername());
        assertEquals(playerUser.getEmail(), result.getEmail());
        assertEquals(playerUser.getFirstName(), result.getFirstName());
        assertEquals(playerUser.getLastName(), result.getLastName());
        assertEquals(playerUser.getUserType(), result.getUserType());
        assertEquals(playerUser.getHandPreference(), result.getHandPreference());

        verify(userRepository).findById(playerUser.getId());
    }

    /**
     * Tests that an exception is thrown when the user is not found by ID.
     */
    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_UserNotFound() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.getUserById(999L)
        );

        verify(userRepository).findById(999L);
    }

    /**
     * Tests retrieving all users successfully.
     */
    @Test
    @DisplayName("Should return all users")
    void getAllUsers_Success() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(adminUser, playerUser, refereeUser));

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertEquals(3, result.size());
        assertEquals(adminUser.getId(), result.get(0).getId());
        assertEquals(playerUser.getId(), result.get(1).getId());
        assertEquals(refereeUser.getId(), result.get(2).getId());

        verify(userRepository).findAll();
    }

    /**
     * Tests retrieving users by type successfully.
     */
    @Test
    @DisplayName("Should return users by type")
    void getUsersByType_Success() {
        // Arrange
        when(userRepository.findByUserType(User.UserType.PLAYER))
                .thenReturn(Collections.singletonList(playerUser));

        // Act
        List<UserDto> result = userService.getUsersByType(User.UserType.PLAYER);

        // Assert
        assertEquals(1, result.size());
        assertEquals(playerUser.getId(), result.get(0).getId());
        assertEquals(playerUser.getUserType(), result.get(0).getUserType());

        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }

    /**
     * Tests successful update of a user's own profile.
     */
    @Test
    @DisplayName("Should update user profile when user updates their own profile")
    void updateUser_SelfUpdate_Success() {
        // Arrange
        UserDto updateDto = new UserDto();
        updateDto.setId(playerUser.getId());
        updateDto.setUsername("updated_player");
        updateDto.setEmail("updated@example.com");
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Player");
        updateDto.setHandPreference(User.HandPreference.LEFT);

        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        when(userRepository.existsByUsername(updateDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(updateDto.getEmail())).thenReturn(false);

        // Mock security context - user updating their own profile
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        User updatedUser = new User();
        updatedUser.setId(playerUser.getId());
        updatedUser.setUsername(updateDto.getUsername());
        updatedUser.setEmail(updateDto.getEmail());
        updatedUser.setFirstName(updateDto.getFirstName());
        updatedUser.setLastName(updateDto.getLastName());
        updatedUser.setUserType(playerUser.getUserType());
        updatedUser.setHandPreference(updateDto.getHandPreference());
        updatedUser.setPassword(playerUser.getPassword());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateUser(playerUser.getId(), updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(updateDto.getUsername(), result.getUsername());
        assertEquals(updateDto.getEmail(), result.getEmail());
        assertEquals(updateDto.getFirstName(), result.getFirstName());
        assertEquals(updateDto.getLastName(), result.getLastName());
        assertEquals(updateDto.getHandPreference(), result.getHandPreference());
        assertEquals(playerUser.getUserType(), result.getUserType()); // User type shouldn't change

        verify(userRepository).findById(playerUser.getId());
        verify(userRepository).existsByUsername(updateDto.getUsername());
        verify(userRepository).existsByEmail(updateDto.getEmail());
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(updateDto.getUsername(), capturedUser.getUsername());
        assertEquals(updateDto.getEmail(), capturedUser.getEmail());
        assertEquals(playerUser.getPassword(), capturedUser.getPassword()); // Password should remain unchanged
    }

    /**
     * Tests that an admin can update another user's role.
     */
    @Test
    @DisplayName("Should allow admin to update user's role")
    void updateUser_AdminCanChangeRole() {
        // Arrange
        UserDto updateDto = new UserDto();
        updateDto.setId(playerUser.getId());
        updateDto.setUsername(playerUser.getUsername());
        updateDto.setEmail(playerUser.getEmail());
        updateDto.setFirstName(playerUser.getFirstName());
        updateDto.setLastName(playerUser.getLastName());
        updateDto.setUserType(User.UserType.REFEREE); // Change role

        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        // Remove unnecessary stubbings

        // Mock security context - admin updating someone else's profile
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adminUser);
        SecurityContextHolder.setContext(securityContext);

        User updatedUser = new User();
        updatedUser.setId(playerUser.getId());
        updatedUser.setUsername(playerUser.getUsername());
        updatedUser.setEmail(playerUser.getEmail());
        updatedUser.setFirstName(playerUser.getFirstName());
        updatedUser.setLastName(playerUser.getLastName());
        updatedUser.setUserType(User.UserType.REFEREE); // Role changed
        updatedUser.setPassword(playerUser.getPassword());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updateUser(playerUser.getId(), updateDto);

        // Assert
        assertEquals(User.UserType.REFEREE, result.getUserType()); // Role should be changed
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(User.UserType.REFEREE, capturedUser.getUserType());
    }

    /**
     * Tests that a non-admin cannot update another user's profile.
     */
    @Test
    @DisplayName("Should not allow non-admin to update another user's profile")
    void updateUser_NonAdminCannotUpdateOthers() {
        // Arrange
        UserDto updateDto = new UserDto();
        updateDto.setId(adminUser.getId());
        updateDto.setUsername(adminUser.getUsername());

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        // Mock security context - player trying to update admin's profile
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                userService.updateUser(adminUser.getId(), updateDto)
        );

        assertEquals("You can only update your own profile.", exception.getMessage());

        verify(userRepository).findById(adminUser.getId());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that an exception is thrown when the username is already taken by another user.
     */
    @Test
    @DisplayName("Should not allow username that is already taken by another user")
    void updateUser_UsernameAlreadyTaken() {
        // Arrange
        UserDto updateDto = new UserDto();
        updateDto.setId(playerUser.getId());
        updateDto.setUsername("admin"); // Taken by another user
        updateDto.setEmail(playerUser.getEmail());

        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        // Mock security context - user updating their own profile
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.updateUser(playerUser.getId(), updateDto)
        );

        assertEquals("Username is already taken", exception.getMessage());

        verify(userRepository).findById(playerUser.getId());
        verify(userRepository).existsByUsername("admin");
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful password update for a user.
     */
    @Test
    @DisplayName("Should update password")
    void updatePassword_Success() {
        // Arrange
        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));

        // Mock security context - user updating their own password
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        // Mock authentication of current password
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(passwordEncoder.encode(passwordUpdateDto.getNewPassword())).thenReturn("new_encoded_password");

        User updatedUser = new User();
        updatedUser.setId(playerUser.getId());
        updatedUser.setUsername(playerUser.getUsername());
        updatedUser.setPassword("new_encoded_password");
        updatedUser.setEmail(playerUser.getEmail());
        updatedUser.setFirstName(playerUser.getFirstName());
        updatedUser.setLastName(playerUser.getLastName());
        updatedUser.setUserType(playerUser.getUserType());

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserDto result = userService.updatePassword(playerUser.getId(), passwordUpdateDto);

        // Assert
        assertNotNull(result);

        verify(userRepository).findById(playerUser.getId());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(passwordEncoder).encode(passwordUpdateDto.getNewPassword());
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("new_encoded_password", capturedUser.getPassword());
    }

    /**
     * Tests that an exception is thrown when the current password is incorrect.
     */
    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void updatePassword_IncorrectCurrentPassword() {
        // Arrange
        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));

        // Mock security context - user updating their own password
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        // Mock authentication failure for current password
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.updatePassword(playerUser.getId(), passwordUpdateDto)
        );

        assertEquals("Current password is incorrect", exception.getMessage());

        verify(userRepository).findById(playerUser.getId());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests that a user cannot update another user's password.
     */
    @Test
    @DisplayName("Should not allow updating password of another user")
    void updatePassword_CannotUpdateOthersPassword() {
        // Arrange
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        // Mock security context - player trying to update admin's password
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(playerUser);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                userService.updatePassword(adminUser.getId(), passwordUpdateDto)
        );

        assertEquals("You can only change your own password.", exception.getMessage());

        verify(userRepository).findById(adminUser.getId());
        verify(authenticationManager, never()).authenticate(any());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful deletion of a user.
     */
    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        when(matchRepository.findByReferee(playerUser)).thenReturn(Collections.emptyList());
        when(registrationRepository.findByPlayer(playerUser)).thenReturn(Collections.emptyList());

        // Act
        userService.deleteUser(playerUser.getId());

        // Assert
        verify(userRepository).findById(playerUser.getId());
        verify(matchRepository).findByReferee(playerUser);
        verify(registrationRepository).findByPlayer(playerUser);
        verify(userRepository).deleteById(playerUser.getId());
    }

    /**
     * Tests that the last admin cannot be deleted.
     */
    @Test
    @DisplayName("Should not delete the last admin")
    void deleteUser_CannotDeleteLastAdmin() {
        // Arrange
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUserType(User.UserType.ADMIN))
                .thenReturn(Collections.singletonList(adminUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.deleteUser(adminUser.getId())
        );

        assertEquals("Cannot delete the last admin user. At least one admin must remain.", exception.getMessage());

        verify(userRepository).findById(adminUser.getId());
        verify(userRepository).findByUserType(User.UserType.ADMIN);
        verify(userRepository, never()).deleteById(anyLong());
    }

    /**
     * Tests that a referee with active matches cannot be deleted.
     */
    @Test
    @DisplayName("Should not delete referee with active matches")
    void deleteUser_CannotDeleteRefereeWithActiveMatches() {
        // Arrange
        Match activeMatch = new Match();
        activeMatch.setId(1L);
        activeMatch.setStatus(Match.MatchStatus.SCHEDULED);

        when(userRepository.findById(refereeUser.getId())).thenReturn(Optional.of(refereeUser));
        when(matchRepository.findByReferee(refereeUser))
                .thenReturn(Collections.singletonList(activeMatch));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.deleteUser(refereeUser.getId())
        );

        assertEquals("Cannot delete referee assigned to ongoing or upcoming matches.", exception.getMessage());

        verify(userRepository).findById(refereeUser.getId());
        verify(matchRepository).findByReferee(refereeUser);
        verify(userRepository, never()).deleteById(anyLong());
    }

    /**
     * Tests that a player with active registrations cannot be deleted.
     */
    @Test
    @DisplayName("Should not delete player with active registrations")
    void deleteUser_CannotDeletePlayerWithActiveRegistrations() {
        // Arrange
        TournamentRegistration activeRegistration = new TournamentRegistration();
        activeRegistration.setId(1L);
        activeRegistration.setPlayer(playerUser);
        activeRegistration.setStatus(TournamentRegistration.RegistrationStatus.APPROVED);

        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Summer Tournament");
        tournament.setStartDate(LocalDate.now().plusDays(10));
        tournament.setEndDate(LocalDate.now().plusDays(15));
        activeRegistration.setTournament(tournament);

        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        when(registrationRepository.findByPlayer(playerUser))
                .thenReturn(Collections.singletonList(activeRegistration));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.deleteUser(playerUser.getId())
        );

        assertEquals("Cannot delete player registered in ongoing tournaments.", exception.getMessage());

        verify(userRepository).findById(playerUser.getId());
        verify(registrationRepository).findByPlayer(playerUser);
        verify(userRepository, never()).deleteById(anyLong());
    }

    /**
     * Tests that a referee with only completed matches can be deleted.
     */
    @Test
    @DisplayName("Can delete referee with only completed matches")
    void deleteUser_CanDeleteRefereeWithCompletedMatches() {
        // Arrange
        Match completedMatch = new Match();
        completedMatch.setId(1L);
        completedMatch.setStatus(Match.MatchStatus.COMPLETED);

        when(userRepository.findById(refereeUser.getId())).thenReturn(Optional.of(refereeUser));
        when(matchRepository.findByReferee(refereeUser))
                .thenReturn(Collections.singletonList(completedMatch));

        // Act
        userService.deleteUser(refereeUser.getId());

        // Assert
        verify(userRepository).findById(refereeUser.getId());
        verify(matchRepository).findByReferee(refereeUser);
        verify(userRepository).deleteById(refereeUser.getId());
    }

    /**
     * Tests that a player with rejected registrations can be deleted.
     */
    @Test
    @DisplayName("Can delete player with rejected registrations")
    void deleteUser_CanDeletePlayerWithRejectedRegistrations() {
        // Arrange
        TournamentRegistration rejectedRegistration = new TournamentRegistration();
        rejectedRegistration.setId(1L);
        rejectedRegistration.setPlayer(playerUser);
        rejectedRegistration.setStatus(TournamentRegistration.RegistrationStatus.REJECTED);

        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Summer Tournament");
        tournament.setStartDate(LocalDate.now().plusDays(10));
        tournament.setEndDate(LocalDate.now().plusDays(15));
        rejectedRegistration.setTournament(tournament);

        when(userRepository.findById(playerUser.getId())).thenReturn(Optional.of(playerUser));
        when(registrationRepository.findByPlayer(playerUser))
                .thenReturn(Collections.singletonList(rejectedRegistration));

        // Act
        userService.deleteUser(playerUser.getId());

        // Assert
        verify(userRepository).findById(playerUser.getId());
        verify(registrationRepository).findByPlayer(playerUser);
        verify(userRepository).deleteById(playerUser.getId());
    }

    /**
     * Tests that an exception is thrown when the user to be deleted is not found.
     */
    @Test
    @DisplayName("Should throw exception when user not found for deletion")
    void deleteUser_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userService.deleteUser(999L)
        );

        verify(userRepository).findById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}