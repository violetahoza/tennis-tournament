package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.TournamentRegistrationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TournamentRegistrationServiceTest {

    @Mock
    private TournamentRegistrationRepository registrationRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TournamentRegistrationService registrationService;

    private User player;
    private User admin;
    private Tournament tournament;
    private TournamentRegistration registration;

    @BeforeEach
    void setUp() {
        // Create test data
        player = new User();
        player.setId(1L);
        player.setFirstName("John");
        player.setLastName("Doe");
        player.setEmail("john.doe@example.com");
        player.setUsername("johndoe");
        player.setUserType(User.UserType.PLAYER);

        admin = new User();
        admin.setId(2L);
        admin.setUserType(User.UserType.ADMIN);

        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Spring Open");
        tournament.setDescription("Annual spring tennis tournament");
        tournament.setLocation("Tennis Club");
        tournament.setStartDate(LocalDate.now().plusDays(15));
        tournament.setEndDate(LocalDate.now().plusDays(20));
        tournament.setRegistrationDeadline(LocalDate.now().plusDays(10));
        tournament.setMaxParticipants(32);

        registration = new TournamentRegistration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus(TournamentRegistration.RegistrationStatus.PENDING);
    }

    @Test
    void getRegistrationsByPlayer_whenPlayerExists_shouldReturnRegistrations() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(player));
        when(registrationRepository.findByPlayer(player)).thenReturn(List.of(registration));

        // Act
        List<TournamentRegistrationDto> result = registrationService.getRegistrationsByPlayer(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Spring Open", result.get(0).getTournamentName());
        assertEquals("John Doe", result.get(0).getPlayerName());
        verify(userRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).findByPlayer(player);
    }

    @Test
    void getRegistrationsByPlayer_whenPlayerNotExists_shouldThrowException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> registrationService.getRegistrationsByPlayer(999L));
        verify(userRepository, times(1)).findById(999L);
        verify(registrationRepository, never()).findByPlayer(any());
    }

    @Test
    void getRegistrationsByTournament_whenTournamentExists_shouldReturnRegistrations() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournament(tournament)).thenReturn(List.of(registration));

        // Act
        List<TournamentRegistrationDto> result = registrationService.getRegistrationsByTournament(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getPlayerName());
        assertEquals("Spring Open", result.get(0).getTournamentName());
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).findByTournament(tournament);
    }

    @Test
    void getRegistrationsByTournament_whenTournamentNotExists_shouldThrowException() {
        // Arrange
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> registrationService.getRegistrationsByTournament(999L));
        verify(tournamentRepository, times(1)).findById(999L);
        verify(registrationRepository, never()).findByTournament(any());
    }

    @Test
    void registerPlayerForTournament_whenValidRequest_shouldCreateRegistration() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByPlayerAndTournament(player, tournament)).thenReturn(Optional.empty());
        when(registrationRepository.countApprovedRegistrationsByTournamentId(1L)).thenReturn(10L);
        when(registrationRepository.save(any(TournamentRegistration.class))).thenReturn(registration);

        // Act
        TournamentRegistrationDto result = registrationService.registerPlayerForTournament(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Spring Open", result.getTournamentName());
        assertEquals("John Doe", result.getPlayerName());
        assertEquals(TournamentRegistration.RegistrationStatus.PENDING, result.getStatus());
        verify(userRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).findByPlayerAndTournament(player, tournament);
        verify(registrationRepository, times(1)).save(any(TournamentRegistration.class));
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    void registerPlayerForTournament_whenNonPlayerUser_shouldThrowException() {
        // Arrange
        User admin = new User();
        admin.setId(2L);
        admin.setUserType(User.UserType.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        // Don't mock tournamentRepository.findById() since we expect it not to be called

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> registrationService.registerPlayerForTournament(2L, 1L));
        verify(userRepository, times(1)).findById(2L);
        verify(tournamentRepository, never()).findById(any()); // Verify it's never called
        verify(registrationRepository, never()).save(any(TournamentRegistration.class));
    }

    @Test
    void registerPlayerForTournament_whenDeadlinePassed_shouldThrowException() {
        // Arrange
        tournament.setRegistrationDeadline(LocalDate.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> registrationService.registerPlayerForTournament(1L, 1L));
        verify(userRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, never()).save(any(TournamentRegistration.class));
    }

    @Test
    void registerPlayerForTournament_whenAlreadyRegistered_shouldThrowException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByPlayerAndTournament(player, tournament)).thenReturn(Optional.of(registration));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> registrationService.registerPlayerForTournament(1L, 1L));
        verify(userRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).findByPlayerAndTournament(player, tournament);
        verify(registrationRepository, never()).save(any(TournamentRegistration.class));
    }

    @Test
    void updateRegistrationStatus_whenRegistrationExists_shouldUpdateStatus() {
        // Arrange
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.save(any(TournamentRegistration.class))).thenReturn(registration);

        // Act
        TournamentRegistrationDto result = registrationService.updateRegistrationStatus(
                1L, TournamentRegistration.RegistrationStatus.APPROVED);

        // Assert
        assertNotNull(result);
        assertEquals(TournamentRegistration.RegistrationStatus.APPROVED, result.getStatus());
        verify(registrationRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).save(any(TournamentRegistration.class));
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    void updateRegistrationStatus_whenRegistrationNotExists_shouldThrowException() {
        // Arrange
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                registrationService.updateRegistrationStatus(999L, TournamentRegistration.RegistrationStatus.APPROVED)
        );
        verify(registrationRepository, times(1)).findById(999L);
        verify(registrationRepository, never()).save(any(TournamentRegistration.class));
    }

    @Test
    void cancelRegistration_whenAuthenticatedAsPlayer_shouldCancelRegistration() {
        // Arrange - Mock security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(player);

        // Mock repository calls
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(userRepository.findByUserType(User.UserType.ADMIN)).thenReturn(List.of(admin));

        // Act
        assertDoesNotThrow(() -> registrationService.cancelRegistration(1L));

        // Assert
        verify(registrationRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).delete(registration);
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    void cancelRegistration_whenTournamentStarted_shouldThrowException() {
        // Arrange - Mock security context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(player);

        // Set tournament to have already started
        tournament.setStartDate(LocalDate.now().minusDays(1));

        // Mock repository calls
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> registrationService.cancelRegistration(1L));
        verify(registrationRepository, times(1)).findById(1L);
        verify(registrationRepository, never()).delete(any());
    }

    @Test
    void countApprovedRegistrationsByTournamentId_whenTournamentExists_shouldReturnCount() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.countByTournamentAndStatus(
                tournament, TournamentRegistration.RegistrationStatus.APPROVED))
                .thenReturn(10L);

        // Act
        long result = registrationService.countApprovedRegistrationsByTournamentId(1L);

        // Assert
        assertEquals(10L, result);
        verify(tournamentRepository, times(1)).findById(1L);
        verify(registrationRepository, times(1)).countByTournamentAndStatus(
                tournament, TournamentRegistration.RegistrationStatus.APPROVED);
    }
}