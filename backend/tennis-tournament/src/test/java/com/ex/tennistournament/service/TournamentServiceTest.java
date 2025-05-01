package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.TournamentDto;
import com.ex.tennistournament.dto.TournamentSummaryDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentRegistrationRepository registrationRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private TournamentService tournamentService;

    private Tournament tournament1;
    private Tournament tournament2;
    private TournamentDto tournamentDto;

    @BeforeEach
    void setUp() {
        // Create test data
        tournament1 = new Tournament();
        tournament1.setId(1L);
        tournament1.setName("Test Tournament 1");
        tournament1.setDescription("Test Description 1");
        tournament1.setLocation("Test Location 1");
        tournament1.setStartDate(LocalDate.now().plusDays(10));
        tournament1.setEndDate(LocalDate.now().plusDays(15));
        tournament1.setRegistrationDeadline(LocalDate.now().plusDays(5));
        tournament1.setMaxParticipants(32);

        tournament2 = new Tournament();
        tournament2.setId(2L);
        tournament2.setName("Test Tournament 2");
        tournament2.setDescription("Test Description 2");
        tournament2.setLocation("Test Location 2");
        tournament2.setStartDate(LocalDate.now().plusDays(20));
        tournament2.setEndDate(LocalDate.now().plusDays(25));
        tournament2.setRegistrationDeadline(LocalDate.now().plusDays(15));
        tournament2.setMaxParticipants(16);

        tournamentDto = TournamentDto.builder()
                .name("New Tournament")
                .description("New Description")
                .location("New Location")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(35))
                .registrationDeadline(LocalDate.now().plusDays(25))
                .maxParticipants(24)
                .build();
    }

    @Test
    void getAllTournaments_shouldReturnAllTournaments() {
        // Arrange
        when(tournamentRepository.findAll()).thenReturn(Arrays.asList(tournament1, tournament2));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(any())).thenReturn(5L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getAllTournaments();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Test Tournament 1", result.get(0).getName());
        assertEquals("Test Tournament 2", result.get(1).getName());
        verify(tournamentRepository, times(1)).findAll();
    }

    @Test
    void getUpcomingTournaments_shouldReturnOnlyUpcomingTournaments() {
        // Arrange
        when(tournamentRepository.findByStartDateAfter(any())).thenReturn(Arrays.asList(tournament1, tournament2));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(any())).thenReturn(5L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getUpcomingTournaments();

        // Assert
        assertEquals(2, result.size());
        verify(tournamentRepository, times(1)).findByStartDateAfter(any());
    }

    @Test
    void getOpenForRegistrationTournaments_shouldReturnOpenTournaments() {
        // Arrange
        when(tournamentRepository.findByRegistrationDeadlineAfter(any())).thenReturn(Arrays.asList(tournament1, tournament2));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(any())).thenReturn(5L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getOpenForRegistrationTournaments();

        // Assert
        assertEquals(2, result.size());
        verify(tournamentRepository, times(1)).findByRegistrationDeadlineAfter(any());
    }

    @Test
    void getTournamentById_whenExists_shouldReturnTournament() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament1));

        // Act
        TournamentDto result = tournamentService.getTournamentById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Tournament 1", result.getName());
        assertEquals("Test Description 1", result.getDescription());
        verify(tournamentRepository, times(1)).findById(1L);
    }

    @Test
    void getTournamentById_whenNotExists_shouldThrowException() {
        // Arrange
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> tournamentService.getTournamentById(999L));
        verify(tournamentRepository, times(1)).findById(999L);
    }

    @Test
    void createTournament_shouldCreateAndReturnTournament() {
        // Arrange
        Tournament savedTournament = new Tournament();
        savedTournament.setId(3L);
        savedTournament.setName(tournamentDto.getName());
        savedTournament.setDescription(tournamentDto.getDescription());
        savedTournament.setLocation(tournamentDto.getLocation());
        savedTournament.setStartDate(tournamentDto.getStartDate());
        savedTournament.setEndDate(tournamentDto.getEndDate());
        savedTournament.setRegistrationDeadline(tournamentDto.getRegistrationDeadline());
        savedTournament.setMaxParticipants(tournamentDto.getMaxParticipants());

        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);

        // Act
        TournamentDto result = tournamentService.createTournament(tournamentDto);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("New Tournament", result.getName());
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void updateTournament_whenExists_shouldUpdateAndReturnTournament() {
        // Arrange
        TournamentDto updateDto = TournamentDto.builder()
                .id(1L)
                .name("Updated Tournament")
                .description("Updated Description")
                .location("Updated Location")
                .startDate(LocalDate.now().plusDays(40))
                .endDate(LocalDate.now().plusDays(45))
                .registrationDeadline(LocalDate.now().plusDays(35))
                .maxParticipants(48)
                .build();

        Tournament updatedTournament = new Tournament();
        updatedTournament.setId(1L);
        updatedTournament.setName(updateDto.getName());
        updatedTournament.setDescription(updateDto.getDescription());
        updatedTournament.setLocation(updateDto.getLocation());
        updatedTournament.setStartDate(updateDto.getStartDate());
        updatedTournament.setEndDate(updateDto.getEndDate());
        updatedTournament.setRegistrationDeadline(updateDto.getRegistrationDeadline());
        updatedTournament.setMaxParticipants(updateDto.getMaxParticipants());

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament1));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);

        // Act
        TournamentDto result = tournamentService.updateTournament(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated Tournament", result.getName());
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void updateTournament_whenNotExists_shouldThrowException() {
        // Arrange
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                tournamentService.updateTournament(999L, tournamentDto)
        );
        verify(tournamentRepository, times(1)).findById(999L);
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }

    @Test
    void deleteTournament_whenValid_shouldDeleteTournament() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament1));
        when(matchRepository.findByTournament(tournament1)).thenReturn(List.of());
        when(registrationRepository.countApprovedRegistrationsByTournamentId(1L)).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> tournamentService.deleteTournament(1L));

        // Assert
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTournament_whenHasRegistrations_shouldThrowException() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament1));
        when(matchRepository.findByTournament(tournament1)).thenReturn(List.of());
        when(registrationRepository.countApprovedRegistrationsByTournamentId(1L)).thenReturn(5L);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tournamentService.deleteTournament(1L));
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, never()).deleteById(1L);
    }
}