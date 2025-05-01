package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.PlayerFilterDto;
import com.ex.tennistournament.dto.PlayerStatisticsDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import com.ex.tennistournament.repository.TournamentRegistrationRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerFilterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TournamentRegistrationRepository tournamentRegistrationRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchScoreRepository matchScoreRepository;

    @InjectMocks
    private PlayerFilterService playerFilterService;

    private User player1;
    private User player2;
    private User player3;
    private Tournament tournament;
    private TournamentRegistration registration1;
    private TournamentRegistration registration2;
    private Match match1;
    private Match match2;
    private MatchScore matchScore1;
    private MatchScore matchScore2;

    @BeforeEach
    void setUp() {
        // Set up players
        player1 = new User();
        player1.setId(1L);
        player1.setUsername("player1");
        player1.setEmail("player1@example.com");
        player1.setFirstName("John");
        player1.setLastName("Doe");
        player1.setUserType(User.UserType.PLAYER);
        player1.setHandPreference(User.HandPreference.RIGHT);

        player2 = new User();
        player2.setId(2L);
        player2.setUsername("player2");
        player2.setEmail("player2@example.com");
        player2.setFirstName("Jane");
        player2.setLastName("Smith");
        player2.setUserType(User.UserType.PLAYER);
        player2.setHandPreference(User.HandPreference.LEFT);

        player3 = new User();
        player3.setId(3L);
        player3.setUsername("player3");
        player3.setEmail("player3@example.com");
        player3.setFirstName("Michael");
        player3.setLastName("Johnson");
        player3.setUserType(User.UserType.PLAYER);
        player3.setHandPreference(User.HandPreference.RIGHT);

        // Set up tournaments
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Summer Tournament");
        tournament.setStartDate(LocalDate.now().plusDays(10));
        tournament.setEndDate(LocalDate.now().plusDays(15));
        tournament.setMaxParticipants(16);

        // Set up registrations
        registration1 = new TournamentRegistration();
        registration1.setId(1L);
        registration1.setPlayer(player1);
        registration1.setTournament(tournament);
        registration1.setStatus(TournamentRegistration.RegistrationStatus.APPROVED);

        registration2 = new TournamentRegistration();
        registration2.setId(2L);
        registration2.setPlayer(player2);
        registration2.setTournament(tournament);
        registration2.setStatus(TournamentRegistration.RegistrationStatus.WAITLISTED);

        // Set up matches
        match1 = new Match();
        match1.setId(1L);
        match1.setTournament(tournament);
        match1.setPlayer1(player1);
        match1.setPlayer2(player2);
        match1.setStatus(Match.MatchStatus.COMPLETED);

        match2 = new Match();
        match2.setId(2L);
        match2.setTournament(tournament);
        match2.setPlayer1(player1);
        match2.setPlayer2(player3);
        match2.setStatus(Match.MatchStatus.COMPLETED);

        // Set up match scores
        matchScore1 = new MatchScore();
        matchScore1.setId(1L);
        matchScore1.setMatch(match1);
        matchScore1.setSetNumber(1);
        matchScore1.setPlayer1Score(6);
        matchScore1.setPlayer2Score(4);

        matchScore2 = new MatchScore();
        matchScore2.setId(2L);
        matchScore2.setMatch(match2);
        matchScore2.setSetNumber(1);
        matchScore2.setPlayer1Score(3);
        matchScore2.setPlayer2Score(6);
    }

    @Test
    @DisplayName("Should return all players")
    void getAllPlayers_Success() {
        // Arrange
        List<User> players = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(players);

        // Act
        List<UserDto> result = playerFilterService.getAllPlayers();

        // Assert
        assertEquals(3, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Jane", result.get(1).getFirstName());
        assertEquals("Michael", result.get(2).getFirstName());
        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }

    @Test
    @DisplayName("Should return empty list when no players found")
    void getAllPlayers_NoPlayersFound() {
        // Arrange
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(Collections.emptyList());

        // Act
        List<UserDto> result = playerFilterService.getAllPlayers();

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }

    @Test
    @DisplayName("Should return players registered for a tournament")
    void getPlayersByTournament_Success() {
        // Arrange
        List<TournamentRegistration> registrations = Arrays.asList(registration1, registration2);
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(tournamentRegistrationRepository.findByTournament(tournament)).thenReturn(registrations);

        // Act
        List<UserDto> result = playerFilterService.getPlayersByTournament(tournament.getId());

        // Assert
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Jane", result.get(1).getFirstName());
        assertEquals(tournament.getId(), result.get(0).getTournamentId());
        assertEquals(tournament.getName(), result.get(0).getTournamentName());
        assertEquals("APPROVED", result.get(0).getTournamentStatus());
        assertEquals("WAITLISTED", result.get(1).getTournamentStatus());
        verify(tournamentRepository).findById(tournament.getId());
        verify(tournamentRegistrationRepository).findByTournament(tournament);
    }

    @Test
    @DisplayName("Should throw exception when tournament not found")
    void getPlayersByTournament_TournamentNotFound() {
        // Arrange
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                playerFilterService.getPlayersByTournament(999L)
        );
        verify(tournamentRegistrationRepository, never()).findByTournament(any());
    }

    @Test
    @DisplayName("Should return players by hand preference")
    void getPlayersByHandPreference_Success() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);

        // Act
        List<UserDto> rightHanded = playerFilterService.getPlayersByHandPreference("RIGHT");
        List<UserDto> leftHanded = playerFilterService.getPlayersByHandPreference("LEFT");

        // Assert
        assertEquals(2, rightHanded.size());
        assertEquals("John", rightHanded.get(0).getFirstName());
        assertEquals("Michael", rightHanded.get(1).getFirstName());
        assertEquals(User.HandPreference.RIGHT, rightHanded.get(0).getHandPreference());

        assertEquals(1, leftHanded.size());
        assertEquals("Jane", leftHanded.get(0).getFirstName());
        assertEquals(User.HandPreference.LEFT, leftHanded.get(0).getHandPreference());

        verify(userRepository, times(2)).findByUserType(User.UserType.PLAYER);
    }

    @Test
    @DisplayName("Should return empty list for invalid hand preference")
    void getPlayersByHandPreference_InvalidPreference() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);

        // Act
        List<UserDto> result = playerFilterService.getPlayersByHandPreference("INVALID");

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }

    @Test
    @DisplayName("Should filter players by hand preference")
    void filterPlayers_ByHandPreference() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);

        PlayerFilterDto filterDto = new PlayerFilterDto();
        filterDto.setHandPreference("LEFT");

        // Act
        List<UserDto> result = playerFilterService.filterPlayers(filterDto);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Jane", result.get(0).getFirstName());
        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }

    @Test
    @DisplayName("Should filter players by tournament and status")
    void filterPlayers_ByTournamentAndStatus() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(tournamentRegistrationRepository.findByTournament(tournament))
                .thenReturn(Arrays.asList(registration1, registration2));

        PlayerFilterDto filterDto = new PlayerFilterDto();
        filterDto.setTournamentId(tournament.getId());
        filterDto.setTournamentStatus("APPROVED");

        // Act
        List<UserDto> result = playerFilterService.filterPlayers(filterDto);

        // Assert
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals(tournament.getId(), result.get(0).getTournamentId());
        assertEquals("APPROVED", result.get(0).getTournamentStatus());
        verify(userRepository).findByUserType(User.UserType.PLAYER);
        verify(tournamentRepository).findById(tournament.getId());
        verify(tournamentRegistrationRepository).findByTournament(tournament);
    }

    @Test
    @DisplayName("Should handle invalid tournament status")
    void filterPlayers_InvalidTournamentStatus() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(tournamentRegistrationRepository.findByTournament(tournament))
                .thenReturn(Arrays.asList(registration1, registration2));

        PlayerFilterDto filterDto = new PlayerFilterDto();
        filterDto.setTournamentId(tournament.getId());
        filterDto.setTournamentStatus("INVALID_STATUS");

        // Act
        List<UserDto> result = playerFilterService.filterPlayers(filterDto);

        // Assert
        assertEquals(2, result.size(), "Should return all players registered for the tournament regardless of invalid status");
        verify(userRepository).findByUserType(User.UserType.PLAYER);
        verify(tournamentRepository).findById(tournament.getId());
        verify(tournamentRegistrationRepository).findByTournament(tournament);
    }

    @Test
    @DisplayName("Should handle tournament not found in filter")
    void filterPlayers_TournamentNotFound() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);
        when(tournamentRepository.findById(anyLong())).thenReturn(Optional.empty());

        PlayerFilterDto filterDto = new PlayerFilterDto();
        filterDto.setTournamentId(999L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                playerFilterService.filterPlayers(filterDto)
        );
        verify(userRepository).findByUserType(User.UserType.PLAYER);
        verify(tournamentRepository).findById(999L);
    }

    @Test
    @DisplayName("Should return player statistics")
    void getPlayerStatistics_Success() {
        // Arrange
        when(userRepository.findById(player1.getId())).thenReturn(Optional.of(player1));
        when(tournamentRegistrationRepository.findByPlayer(player1))
                .thenReturn(Collections.singletonList(registration1));
        when(matchRepository.findByPlayer1OrPlayer2(player1, player1))
                .thenReturn(Arrays.asList(match1, match2));

        // For first match, player1 wins
        when(matchScoreRepository.findByMatchOrderBySetNumber(match1))
                .thenReturn(Collections.singletonList(matchScore1));

        // For second match, player3 wins
        when(matchScoreRepository.findByMatchOrderBySetNumber(match2))
                .thenReturn(Collections.singletonList(matchScore2));

        // Act
        PlayerStatisticsDto result = playerFilterService.getPlayerStatistics(player1.getId());

        // Assert
        assertNotNull(result);
        assertEquals(player1.getId(), result.getPlayerId());
        assertEquals("John Doe", result.getPlayerName());
        assertEquals(2, result.getTotalMatches());
        assertEquals(2, result.getCompletedMatches());
        assertEquals(1, result.getWins());
        assertEquals(1, result.getLosses());
        assertEquals(1, result.getTournaments());
        assertEquals(50, result.getWinRate());

        verify(userRepository).findById(player1.getId());
        verify(tournamentRegistrationRepository).findByPlayer(player1);
        verify(matchRepository).findByPlayer1OrPlayer2(player1, player1);
        verify(matchScoreRepository).findByMatchOrderBySetNumber(match1);
        verify(matchScoreRepository).findByMatchOrderBySetNumber(match2);
    }

    @Test
    @DisplayName("Should throw exception when player not found")
    void getPlayerStatistics_PlayerNotFound() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                playerFilterService.getPlayerStatistics(999L)
        );
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should throw exception when user is not a player")
    void getPlayerStatistics_NotAPlayer() {
        // Arrange
        User admin = new User();
        admin.setId(4L);
        admin.setUserType(User.UserType.ADMIN);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                playerFilterService.getPlayerStatistics(admin.getId())
        );
        verify(userRepository).findById(admin.getId());
    }

    @Test
    @DisplayName("Should calculate correct win rate with no completed matches")
    void getPlayerStatistics_NoCompletedMatches() {
        // Arrange
        when(userRepository.findById(player1.getId())).thenReturn(Optional.of(player1));
        when(tournamentRegistrationRepository.findByPlayer(player1))
                .thenReturn(Collections.singletonList(registration1));
        when(matchRepository.findByPlayer1OrPlayer2(player1, player1))
                .thenReturn(Collections.emptyList());

        // Act
        PlayerStatisticsDto result = playerFilterService.getPlayerStatistics(player1.getId());

        // Assert
        assertEquals(0, result.getWinRate());
        assertEquals(0, result.getWins());
        assertEquals(0, result.getLosses());
        assertEquals(0, result.getCompletedMatches());
        assertEquals(0, result.getTotalMatches());
        assertEquals(1, result.getTournaments());
    }

    @Test
    @DisplayName("Should handle matches without scores")
    void getPlayerStatistics_MatchesWithoutScores() {
        // Arrange
        Match matchWithoutScores = new Match();
        matchWithoutScores.setId(3L);
        matchWithoutScores.setPlayer1(player1);
        matchWithoutScores.setPlayer2(player2);
        matchWithoutScores.setStatus(Match.MatchStatus.COMPLETED);

        when(userRepository.findById(player1.getId())).thenReturn(Optional.of(player1));
        when(tournamentRegistrationRepository.findByPlayer(player1))
                .thenReturn(Collections.singletonList(registration1));
        when(matchRepository.findByPlayer1OrPlayer2(player1, player1))
                .thenReturn(Collections.singletonList(matchWithoutScores));
        when(matchScoreRepository.findByMatchOrderBySetNumber(matchWithoutScores))
                .thenReturn(Collections.emptyList());

        // Act
        PlayerStatisticsDto result = playerFilterService.getPlayerStatistics(player1.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getCompletedMatches());
        // Should use match ID as deterministic way to assign win/loss when no scores
        assertTrue(result.getWins() == 1 || result.getLosses() == 1);
        assertEquals(1, result.getWins() + result.getLosses(), "Total of wins and losses should be 1");
        // Win rate should be either 0% or 100%
        assertTrue(result.getWinRate() == 0 || result.getWinRate() == 100);
    }

    @Test
    @DisplayName("Should filter players by search term with partial match")
    void filterPlayers_ByPartialSearchTerm() {
        // Arrange
        List<User> allPlayers = Arrays.asList(player1, player2, player3);
        when(userRepository.findByUserType(User.UserType.PLAYER)).thenReturn(allPlayers);

        PlayerFilterDto filterDto = new PlayerFilterDto();
        filterDto.setSearchTerm("jo"); // Partial match for "John" and "Johnson"

        // Act
        List<UserDto> result = playerFilterService.filterPlayers(filterDto);

        // Assert
        assertEquals(2, result.size(), "Two players should match the partial search term 'jo'");
        assertTrue(result.stream().anyMatch(p -> p.getFirstName().equals("John")), "John should be in the results");
        assertTrue(result.stream().anyMatch(p -> p.getLastName().equals("Johnson")), "Johnson should be in the results");
        verify(userRepository).findByUserType(User.UserType.PLAYER);
    }
}