package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.*;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.*;
import com.ex.tennistournament.repository.*;
import com.ex.tennistournament.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link MatchService} class.
 * This class uses Mockito to mock dependencies and test the behavior of the MatchService.
 */
@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchScoreRepository matchScoreRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TournamentRegistrationRepository registrationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private MatchService matchService;

    private Tournament tournament;
    private User player1;
    private User player2;
    private User referee;
    private Match match;
    private MatchDto matchDto;

    /**
     * Sets up the test environment before each test.
     * Initializes test data and mocks.
     */
    @BeforeEach
    void setUp() {
        // Setup tournament
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Wimbledon");
        tournament.setStartDate(LocalDate.now().plusDays(1));
        tournament.setEndDate(LocalDate.now().plusDays(7));

        // Setup players
        player1 = new User();
        player1.setId(1L);
        player1.setFirstName("Roger");
        player1.setLastName("Federer");
        player1.setUserType(User.UserType.PLAYER);

        player2 = new User();
        player2.setId(2L);
        player1.setFirstName("Rafael");
        player1.setLastName("Nadal");
        player2.setUserType(User.UserType.PLAYER);

        // Setup referee
        referee = new User();
        referee.setId(3L);
        referee.setFirstName("Carlos");
        referee.setLastName("Ramos");
        referee.setUserType(User.UserType.REFEREE);

        // Setup match
        match = new Match();
        match.setId(1L);
        match.setTournament(tournament);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setReferee(referee);
        match.setCourtNumber(1);
        match.setScheduledTime(LocalDateTime.now().plusDays(2));
        match.setStatus(Match.MatchStatus.SCHEDULED);
        match.setRound(Match.Round.ROUND_1);

        // Setup match DTO
        matchDto = MatchDto.builder()
                .tournamentId(1L)
                .player1Id(1L)
                .player2Id(2L)
                .refereeId(3L)
                .courtNumber(1)
                .scheduledTime(LocalDateTime.now().plusDays(2))
                .status(Match.MatchStatus.SCHEDULED)
                .round(Match.Round.ROUND_1)
                .build();
    }

    /**
     * Tests retrieving all matches.
     * Verifies that the correct list of matches is returned.
     */
    @Test
    void getAllMatches_shouldReturnAllMatches() {
        // Arrange
        when(matchRepository.findAll()).thenReturn(List.of(match));

        // Act
        List<MatchDto> result = matchService.getAllMatches();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Wimbledon", result.get(0).getTournamentName());
        verify(matchRepository, times(1)).findAll();
    }

    /**
     * Tests retrieving matches by tournament ID.
     * Verifies that the correct matches are returned for the tournament.
     */
    @Test
    void getMatchesByTournament_shouldReturnTournamentMatches() {
        // Arrange
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournament(tournament)).thenReturn(List.of(match));

        // Act
        List<MatchDto> result = matchService.getMatchesByTournament(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Wimbledon", result.get(0).getTournamentName());
        verify(tournamentRepository, times(1)).findById(1L);
        verify(matchRepository, times(1)).findByTournament(tournament);
    }

    /**
     * Tests retrieving matches by referee ID.
     * Verifies that the correct matches are returned for the referee.
     */
    @Test
    void getMatchesByReferee_shouldReturnRefereeMatches() {
        // Arrange
        when(userRepository.findById(3L)).thenReturn(Optional.of(referee));
        when(matchRepository.findByReferee(referee)).thenReturn(List.of(match));

        // Act
        List<MatchDto> result = matchService.getMatchesByReferee(3L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Carlos Ramos", result.get(0).getRefereeName());
        verify(userRepository, times(1)).findById(3L);
        verify(matchRepository, times(1)).findByReferee(referee);
    }

    /**
     * Tests retrieving a match by its ID.
     * Verifies that the correct match is returned when it exists.
     */
    @Test
    void getMatchById_shouldReturnMatch() {
        // Arrange
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        // Act
        MatchDto result = matchService.getMatchById(1L);

        // Assert
        assertEquals("Wimbledon", result.getTournamentName());
        verify(matchRepository, times(1)).findById(1L);
    }

    /**
     * Tests deleting a scheduled match.
     * Verifies that the match is deleted successfully and notifications are sent.
     */
    @Test
    @Transactional
    void deleteMatch_shouldDeleteScheduledMatch() {
        // Arrange
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatch(match)).thenReturn(List.of());

        // Act
        assertDoesNotThrow(() -> matchService.deleteMatch(1L));

        // Assert
        verify(matchRepository, times(1)).deleteById(1L);
        verify(notificationService, atLeast(3)).sendNotification(any());
    }

    /**
     * Tests deleting a match that is in progress.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    @Transactional
    void deleteMatch_whenInProgress_shouldThrowException() {
        // Arrange
        match.setStatus(Match.MatchStatus.IN_PROGRESS);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> matchService.deleteMatch(1L));
        verify(matchRepository, never()).deleteById(any());
    }

    /**
     * Tests deleting a match that has scores.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    @Transactional
    void deleteMatch_whenHasScores_shouldThrowException() {
        // Arrange
        MatchScore score = new MatchScore();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatch(match)).thenReturn(List.of(score));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> matchService.deleteMatch(1L));
        verify(matchRepository, never()).deleteById(any());
    }

}