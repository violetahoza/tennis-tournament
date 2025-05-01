package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private MatchService matchService;

    @InjectMocks
    private ReportService reportService;

    private MatchDto match1;
    private MatchDto match2;

    @BeforeEach
    void setUp() {
        // Setup test data
        match1 = MatchDto.builder()
                .id(1L)
                .tournamentName("Summer Grand Slam")
                .round(Match.Round.ROUND_1)
                .player1Name("Roger Federer")
                .player2Name("Rafael Nadal")
                .refereeName("Carlos Ramos")
                .courtNumber(1)
                .scheduledTime(LocalDateTime.of(2023, 6, 15, 14, 0))
                .status(Match.MatchStatus.SCHEDULED)
                .build();

        match2 = MatchDto.builder()
                .id(2L)
                .tournamentName("Summer Grand Slam")
                .round(Match.Round.ROUND_1)
                .player1Name("Novak Djokovic")
                .player2Name("Andy Murray")
                .refereeName("Eva Asderaki")
                .courtNumber(2)
                .scheduledTime(LocalDateTime.of(2023, 6, 15, 16, 0))
                .status(Match.MatchStatus.COMPLETED)
                .build();
    }

    @Test
    void generateMatchesCSV_shouldReturnValidCSV() throws IOException {
        // Arrange
        List<MatchDto> matches = List.of(match1, match2);

        // Act
        String csvContent = reportService.generateMatchesCSV(matches);

        // Assert
        assertNotNull(csvContent);
        assertTrue(csvContent.contains("Match ID,Tournament,Round,Player 1,Player 2,Referee,Court,Scheduled Time,Status"));
        assertTrue(csvContent.contains("1,Summer Grand Slam,ROUND_1,Roger Federer,Rafael Nadal,Carlos Ramos,1,2023-06-15 14:00,SCHEDULED"));
        assertTrue(csvContent.contains("2,Summer Grand Slam,ROUND_1,Novak Djokovic,Andy Murray,Eva Asderaki,2,2023-06-15 16:00,COMPLETED"));
    }

    @Test
    void generateMatchesTXT_shouldReturnValidTextReport() {
        // Arrange
        List<MatchDto> matches = List.of(match1, match2);

        // Act
        String txtContent = reportService.generateMatchesTXT(matches);

        // Assert
        assertNotNull(txtContent);
        assertTrue(txtContent.contains("TENNIS TOURNAMENT MATCHES REPORT"));
        assertTrue(txtContent.contains("Match ID: 1"));
        assertTrue(txtContent.contains("Tournament: Summer Grand Slam"));
        assertTrue(txtContent.contains("Players: Roger Federer vs Rafael Nadal"));
        assertTrue(txtContent.contains("Referee: Carlos Ramos"));
        assertTrue(txtContent.contains("Scheduled: 2023-06-15 14:00"));
        assertTrue(txtContent.contains("Status: SCHEDULED"));
        assertTrue(txtContent.contains("Match ID: 2"));
        assertTrue(txtContent.contains("Status: COMPLETED"));
    }

    @Test
    void generateMatchesCSV_withEmptyList_shouldReturnOnlyHeader() throws IOException {
        // Arrange
        List<MatchDto> matches = List.of();

        // Act
        String csvContent = reportService.generateMatchesCSV(matches);

        // Assert
        assertNotNull(csvContent);
        assertEquals("Match ID,Tournament,Round,Player 1,Player 2,Referee,Court,Scheduled Time,Status\n", csvContent);
    }

    @Test
    void generateMatchesTXT_withEmptyList_shouldReturnHeaderOnly() {
        // Arrange
        List<MatchDto> matches = List.of();

        // Act
        String txtContent = reportService.generateMatchesTXT(matches);

        // Assert
        assertNotNull(txtContent);
        assertTrue(txtContent.contains("TENNIS TOURNAMENT MATCHES REPORT"));
        assertTrue(txtContent.contains("================================="));
        assertFalse(txtContent.contains("Match ID:"));
    }

    @Test
    void generateMatchesCSV_withNullValues_shouldHandleGracefully() throws IOException {
        // Arrange
        MatchDto matchWithNulls = MatchDto.builder()
                .id(3L)
                .tournamentName(null)
                .round(null)
                .player1Name("Player A")
                .player2Name(null)
                .refereeName(null)
                .courtNumber(null)
                .scheduledTime(LocalDateTime.of(2023, 6, 16, 10, 0))
                .status(null)
                .build();

        List<MatchDto> matches = List.of(matchWithNulls);

        // Act
        String csvContent = reportService.generateMatchesCSV(matches);

        // Assert
        assertNotNull(csvContent);
        assertTrue(csvContent.contains("3,,,Player A,,,," + "2023-06-16 10:00,"));
    }

    @Test
    void generateMatchesTXT_withNullValues_shouldHandleGracefully() {
        // Arrange
        MatchDto matchWithNulls = MatchDto.builder()
                .id(3L)
                .tournamentName(null)
                .round(null)
                .player1Name("Player A")
                .player2Name(null)
                .refereeName(null)
                .courtNumber(null)
                .scheduledTime(LocalDateTime.of(2023, 6, 16, 10, 0))
                .status(null)
                .build();

        List<MatchDto> matches = List.of(matchWithNulls);

        // Act
        String txtContent = reportService.generateMatchesTXT(matches);

        // Assert
        assertNotNull(txtContent);
        assertTrue(txtContent.contains("Match ID: 3"));
        assertTrue(txtContent.contains("Tournament: null"));
        assertTrue(txtContent.contains("Players: Player A vs null"));
        assertTrue(txtContent.contains("Referee: null"));
        assertTrue(txtContent.contains("Scheduled: 2023-06-16 10:00"));
        assertTrue(txtContent.contains("Status: null"));
    }
}