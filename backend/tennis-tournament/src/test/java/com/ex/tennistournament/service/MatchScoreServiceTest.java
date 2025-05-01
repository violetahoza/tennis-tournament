package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.dto.NotificationDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.observer.MatchScoreLogger;
import com.ex.tennistournament.observer.MatchScoreSubject;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import com.ex.tennistournament.repository.UserRepository;
import com.ex.tennistournament.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link MatchScoreService} class.
 * This class uses Mockito to mock dependencies and test the behavior of the MatchScoreService.
 */
@ExtendWith(MockitoExtension.class)
public class MatchScoreServiceTest {
    @Mock
    private MatchScoreRepository matchScoreRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchScoreSubject matchScoreSubject;
    @Mock
    private MatchScoreLogger matchScoreLogger;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @InjectMocks
    private MatchScoreService matchScoreService;

    private Match match;
    private User player1;
    private User player2;
    private User referee;
    private MatchScore matchScore;
    private MatchScoreDto matchScoreDto;

    /**
     * Sets up the test environment before each test.
     * Initializes test data and mocks.
     */
    @BeforeEach
    void setUp() {
        // Initialize test data
        player1 = new User();
        player1.setId(1L);
        player1.setFirstName("Serena");
        player1.setLastName("Williams");
        player1.setUserType(User.UserType.PLAYER);

        player2 = new User();
        player2.setId(2L);
        player2.setFirstName("Naomi");
        player2.setLastName("Osaka");
        player2.setUserType(User.UserType.PLAYER);

        referee = new User();
        referee.setId(3L);
        referee.setFirstName("Mary");
        referee.setLastName("Smith");
        referee.setUserType(User.UserType.REFEREE);

        // Create tournament for the match
        com.ex.tennistournament.model.Tournament tournament = new com.ex.tennistournament.model.Tournament();
        tournament.setId(1L);
        tournament.setName("Australian Open");

        match = new Match();
        match.setId(1L);
        match.setTournament(tournament); // Set tournament to avoid NPE
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setReferee(referee);
        match.setStatus(Match.MatchStatus.IN_PROGRESS);

        matchScore = new MatchScore();
        matchScore.setId(1L);
        matchScore.setMatch(match);
        matchScore.setSetNumber(1);
        matchScore.setPlayer1Score(6);
        matchScore.setPlayer2Score(4);

        matchScoreDto = new MatchScoreDto();
        matchScoreDto.setId(1L);
        matchScoreDto.setMatchId(1L);
        matchScoreDto.setSetNumber(1);
        matchScoreDto.setPlayer1Score(6);
        matchScoreDto.setPlayer2Score(4);

        // Setup security context with referee as authenticated user
        Authentication authentication = new UsernamePasswordAuthenticationToken(referee, null);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Tests retrieving scores by match ID.
     * Verifies that the correct scores are returned.
     */
    @Test
    void getScoresByMatch_ShouldReturnScores() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatchOrderBySetNumber(any(Match.class)))
                .thenReturn(Arrays.asList(matchScore));

        // Act
        List<MatchScoreDto> result = matchScoreService.getScoresByMatch(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(6, result.get(0).getPlayer1Score());
        assertEquals(4, result.get(0).getPlayer2Score());
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, times(1)).findByMatchOrderBySetNumber(match);
    }

    /**
     * Tests retrieving a score by its ID.
     * Verifies that the correct score is returned when it exists.
     */
    @Test
    void getScoreById_ShouldReturnScore_WhenExists() {
        // Arrange
        when(matchScoreRepository.findById(anyLong())).thenReturn(Optional.of(matchScore));

        // Act
        MatchScoreDto result = matchScoreService.getScoreById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getPlayer1Score());
        assertEquals(4, result.getPlayer2Score());
        verify(matchScoreRepository, times(1)).findById(1L);
    }

    /**
     * Tests retrieving a score by its ID when it does not exist.
     * Verifies that a ResourceNotFoundException is thrown.
     */
    @Test
    void getScoreById_ShouldThrowException_WhenNotExists() {
        // Arrange
        when(matchScoreRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            matchScoreService.getScoreById(1L);
        });
        verify(matchScoreRepository, times(1)).findById(1L);
    }

    /**
     * Tests creating a new score for a match.
     * Verifies that the score is created successfully when input is valid.
     */
    @Test
    void createScore_ShouldReturnCreatedScore_WhenValidInput() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatchOrderBySetNumber(any(Match.class)))
                .thenReturn(Collections.emptyList());
        when(matchScoreRepository.save(any(MatchScore.class))).thenReturn(matchScore);

        // Act
        MatchScoreDto result = matchScoreService.createScore(matchScoreDto);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getPlayer1Score());
        assertEquals(4, result.getPlayer2Score());
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, times(1)).findByMatchOrderBySetNumber(match);
        verify(matchScoreRepository, times(1)).save(any(MatchScore.class));
        verify(matchScoreSubject, times(1)).scoreAdded(eq(match), any(MatchScore.class));
    }

    /**
     * Tests creating a score when the match is not found.
     * Verifies that a ResourceNotFoundException is thrown.
     */
    @Test
    void createScore_ShouldThrowException_WhenMatchNotFound() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            matchScoreService.createScore(matchScoreDto);
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, never()).save(any(MatchScore.class));
    }

    /**
     * Tests creating a score when the match is already completed.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    void createScore_ShouldThrowException_WhenMatchIsCompleted() {
        // Arrange
        match.setStatus(Match.MatchStatus.COMPLETED);
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            matchScoreService.createScore(matchScoreDto);
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, never()).save(any(MatchScore.class));
    }

    /**
     * Tests creating a score when the user is not a referee or admin.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    void createScore_ShouldThrowException_WhenNotRefereeOrAdmin() {
        // Arrange
        User player = new User();
        player.setId(4L);
        player.setUserType(User.UserType.PLAYER);

        // Set player as authenticated user
        Authentication playerAuth = new UsernamePasswordAuthenticationToken(player, null);
        when(securityContext.getAuthentication()).thenReturn(playerAuth);

        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            matchScoreService.createScore(matchScoreDto);
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, never()).save(any(MatchScore.class));
    }

    /**
     * Tests creating a score when the user is not a referee or admin.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    void createScore_ShouldThrowException_WhenScoreAlreadyExistsForSet() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatchOrderBySetNumber(any(Match.class)))
                .thenReturn(Arrays.asList(matchScore)); // Existing score for set 1

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            matchScoreService.createScore(matchScoreDto); // Also for set 1
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchScoreRepository, times(1)).findByMatchOrderBySetNumber(match);
        verify(matchScoreRepository, never()).save(any(MatchScore.class));
    }

    /**
     * Tests updating an existing score.
     * Verifies that the score is updated successfully when input is valid.
     */
    @Test
    void updateScore_ShouldReturnUpdatedScore_WhenValidInput() {
        // Arrange
        when(matchScoreRepository.findById(anyLong())).thenReturn(Optional.of(matchScore));
        when(matchScoreRepository.save(any(MatchScore.class))).thenReturn(matchScore);

        // Create a DTO with updated scores
        MatchScoreDto updateDto = new MatchScoreDto();
        updateDto.setId(1L);
        updateDto.setMatchId(1L);
        updateDto.setSetNumber(1);
        updateDto.setPlayer1Score(7);
        updateDto.setPlayer2Score(5);

        // Act
        MatchScoreDto result = matchScoreService.updateScore(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(7, result.getPlayer1Score()); // Should reflect the updated value from the mock
        assertEquals(5, result.getPlayer2Score()); // Should reflect the updated value from the mock
        verify(matchScoreRepository, times(1)).findById(1L);
        verify(matchScoreRepository, times(1)).save(any(MatchScore.class));
        verify(matchScoreSubject, times(1)).scoreUpdated(eq(match), any(MatchScore.class));
    }

    /**
     * Tests completing a match.
     * Verifies that the match is marked as completed and notifications are sent.
     */
    @Test
    void completeMatch_ShouldSucceed_WhenValidInput() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));

        // Create scores with a clear winner (player1 wins both sets)
        MatchScore score1 = new MatchScore();
        score1.setMatch(match);
        score1.setSetNumber(1);
        score1.setPlayer1Score(6);
        score1.setPlayer2Score(4);

        MatchScore score2 = new MatchScore();
        score2.setMatch(match);
        score2.setSetNumber(2);
        score2.setPlayer1Score(6);
        score2.setPlayer2Score(3);

        when(matchScoreRepository.findByMatch(match)).thenReturn(Arrays.asList(score1, score2));

        // Mock userRepository to return a list of admin users for notification
        User admin = new User();
        admin.setId(4L);
        admin.setUserType(User.UserType.ADMIN);
        when(userRepository.findByUserType(User.UserType.ADMIN)).thenReturn(Collections.singletonList(admin));

        // Mock notificationService to avoid actual notifications
        doNothing().when(notificationService).sendNotification(any(NotificationDto.class));

        // Act
        matchScoreService.completeMatch(1L);

        // Assert
        assertEquals(Match.MatchStatus.COMPLETED, match.getStatus());
        verify(matchRepository, times(1)).findById(1L);
        verify(matchRepository, times(1)).save(match);
        verify(matchScoreSubject, times(1)).matchCompleted(match);
        verify(userRepository, times(1)).findByUserType(User.UserType.ADMIN);
        verify(notificationService, times(1)).sendNotification(any(NotificationDto.class));
    }

    /**
     * Tests completing a match when no scores exist.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    void completeMatch_ShouldThrowException_WhenNoScores() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatch(match)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            matchScoreService.completeMatch(1L);
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchRepository, never()).save(any(Match.class));
    }

    /**
     * Tests completing a match when scores are tied.
     * Verifies that an IllegalStateException is thrown.
     */
    @Test
    void completeMatch_ShouldThrowException_WhenScoresTied() {
        // Arrange
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));

        // Create scores with a tie (each player wins one set)
        MatchScore score1 = new MatchScore();
        score1.setMatch(match);
        score1.setSetNumber(1);
        score1.setPlayer1Score(6);
        score1.setPlayer2Score(4);

        MatchScore score2 = new MatchScore();
        score2.setMatch(match);
        score2.setSetNumber(2);
        score2.setPlayer1Score(4);
        score2.setPlayer2Score(6);

        when(matchScoreRepository.findByMatch(match)).thenReturn(Arrays.asList(score1, score2));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            matchScoreService.completeMatch(1L);
        });
        verify(matchRepository, times(1)).findById(1L);
        verify(matchRepository, never()).save(any(Match.class));
    }

    /**
     * Tests validating a tennis set score.
     * Verifies that an exception is thrown for invalid scores.
     */
    @Test
    void validateTennisSetScore_ShouldThrowException_ForInvalidScores() {
        // Create a DTO with invalid scores (tie)
        MatchScoreDto invalidDto = new MatchScoreDto();
        invalidDto.setMatchId(1L);
        invalidDto.setSetNumber(1);
        invalidDto.setPlayer1Score(6);
        invalidDto.setPlayer2Score(6); // Tie score is invalid

        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(match));
        when(matchScoreRepository.findByMatchOrderBySetNumber(any(Match.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            matchScoreService.createScore(invalidDto);
        });
    }
}