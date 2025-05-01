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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    private Tournament tournament;
    private TournamentDto tournamentDto;

    @BeforeEach
    void setUp() {
        // Initialize test data
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Wimbledon");
        tournament.setDescription("Grand Slam Tournament");
        tournament.setLocation("London, UK");
        tournament.setStartDate(LocalDate.now().plusDays(10));
        tournament.setEndDate(LocalDate.now().plusDays(24));
        tournament.setRegistrationDeadline(LocalDate.now().plusDays(5));
        tournament.setMaxParticipants(128);

        tournamentDto = new TournamentDto();
        tournamentDto.setId(1L);
        tournamentDto.setName("Wimbledon");
        tournamentDto.setDescription("Grand Slam Tournament");
        tournamentDto.setLocation("London, UK");
        tournamentDto.setStartDate(LocalDate.now().plusDays(10));
        tournamentDto.setEndDate(LocalDate.now().plusDays(24));
        tournamentDto.setRegistrationDeadline(LocalDate.now().plusDays(5));
        tournamentDto.setMaxParticipants(128);
    }

    @Test
    void getAllTournaments_ShouldReturnListOfTournaments() {
        // Arrange
        when(tournamentRepository.findAll()).thenReturn(Arrays.asList(tournament));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(anyLong())).thenReturn(10L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getAllTournaments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Wimbledon", result.get(0).getName());
        assertEquals("London, UK", result.get(0).getLocation());
        verify(tournamentRepository, times(1)).findAll();
    }

    @Test
    void getUpcomingTournaments_ShouldReturnTournamentsWithStartDateInFuture() {
        // Arrange
        when(tournamentRepository.findByStartDateAfter(any(LocalDate.class))).thenReturn(Arrays.asList(tournament));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(anyLong())).thenReturn(10L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getUpcomingTournaments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Wimbledon", result.get(0).getName());
        verify(tournamentRepository, times(1)).findByStartDateAfter(any(LocalDate.class));
    }

    @Test
    void getOpenForRegistrationTournaments_ShouldReturnTournamentsWithRegistrationDeadlineInFuture() {
        // Arrange
        when(tournamentRepository.findByRegistrationDeadlineAfter(any(LocalDate.class))).thenReturn(Arrays.asList(tournament));
        when(registrationRepository.countApprovedRegistrationsByTournamentId(anyLong())).thenReturn(10L);

        // Act
        List<TournamentSummaryDto> result = tournamentService.getOpenForRegistrationTournaments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Wimbledon", result.get(0).getName());
        verify(tournamentRepository, times(1)).findByRegistrationDeadlineAfter(any(LocalDate.class));
    }

    @Test
    void getTournamentById_ShouldReturnTournament_WhenExists() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));

        // Act
        TournamentDto result = tournamentService.getTournamentById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Wimbledon", result.getName());
        assertEquals("London, UK", result.getLocation());
        verify(tournamentRepository, times(1)).findById(1L);
    }

    @Test
    void getTournamentById_ShouldThrowException_WhenNotExists() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            tournamentService.getTournamentById(1L);
        });
        verify(tournamentRepository, times(1)).findById(1L);
    }

    @Test
    void createTournament_ShouldReturnCreatedTournament() {
        // Arrange
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

        // Act
        TournamentDto result = tournamentService.createTournament(tournamentDto);

        // Assert
        assertNotNull(result);
        assertEquals("Wimbledon", result.getName());
        assertEquals("London, UK", result.getLocation());
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void updateTournament_ShouldReturnUpdatedTournament_WhenTournamentExists() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(tournament);

        tournamentDto.setName("Updated Wimbledon");
        tournamentDto.setDescription("Updated Description");

        // Act
        TournamentDto result = tournamentService.updateTournament(1L, tournamentDto);

        // Assert
        assertNotNull(result);
        assertEquals("Wimbledon", result.getName()); // Name should match the mock's return value
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).save(any(Tournament.class));
    }

    @Test
    void updateTournament_ShouldThrowException_WhenTournamentNotExists() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            tournamentService.updateTournament(1L, tournamentDto);
        });
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }

    @Test
    void deleteTournament_ShouldSucceed_WhenTournamentExistsAndCanBeDeleted() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournament(any(Tournament.class))).thenReturn(Collections.emptyList());
        when(registrationRepository.countApprovedRegistrationsByTournamentId(anyLong())).thenReturn(0L);

        // Set tournament date in the future (not started)
        tournament.setStartDate(LocalDate.now().plusDays(5));

        // Act
        tournamentService.deleteTournament(1L);

        // Assert
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTournament_ShouldThrowException_WhenTournamentAlreadyStarted() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));

        // Set tournament date to today (already started)
        tournament.setStartDate(LocalDate.now());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            tournamentService.deleteTournament(1L);
        });
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteTournament_ShouldThrowException_WhenTournamentHasMatches() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));

        // Set tournament date in the future (not started)
        tournament.setStartDate(LocalDate.now().plusDays(5));

        // Mock that tournament has matches
        when(matchRepository.findByTournament(any(Tournament.class))).thenReturn(Arrays.asList(new com.ex.tennistournament.model.Match()));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            tournamentService.deleteTournament(1L);
        });
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteTournament_ShouldThrowException_WhenTournamentHasRegistrations() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.of(tournament));

        // Set tournament date in the future (not started)
        tournament.setStartDate(LocalDate.now().plusDays(5));

        // Mock empty matches but has registrations
        when(matchRepository.findByTournament(any(Tournament.class))).thenReturn(Collections.emptyList());
        when(registrationRepository.countApprovedRegistrationsByTournamentId(anyLong())).thenReturn(5L);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            tournamentService.deleteTournament(1L);
        });
        verify(tournamentRepository, times(1)).findById(1L);
        verify(tournamentRepository, never()).deleteById(anyLong());
    }
}