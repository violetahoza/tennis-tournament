package com.ex.tennistournament.service;

import com.opencsv.CSVWriter;
import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.dto.MatchSummaryDto;
import com.ex.tennistournament.model.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for generating match reports in various formats.
 * Provides functionality to export match data in CSV and TXT formats.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final MatchService matchService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String generateMatchesCSV(List<MatchDto> matches) throws IOException {
        StringWriter stringWriter = new StringWriter();
        writeMatchesToCSV(stringWriter, matches);
        return stringWriter.toString();
    }

    public String generateMatchesTXT(List<MatchDto> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("TENNIS TOURNAMENT MATCHES REPORT\n");
        sb.append("=================================\n\n");

        for (MatchDto match : matches) {
            sb.append("Match ID: ").append(match.getId()).append("\n");
            sb.append("Tournament: ").append(match.getTournamentName()).append("\n");
            sb.append("Round: ").append(match.getRound()).append("\n");
            sb.append("Players: ").append(match.getPlayer1Name()).append(" vs ").append(match.getPlayer2Name()).append("\n");
            sb.append("Referee: ").append(match.getRefereeName()).append("\n");
            sb.append("Scheduled: ").append(match.getScheduledTime().format(DATE_TIME_FORMATTER)).append("\n");
            sb.append("Status: ").append(match.getStatus()).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    private void writeMatchesToCSV(Writer writer, List<MatchDto> matches) throws IOException {
        CSVWriter csvWriter = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        // Write header
        String[] header = {"Match ID", "Tournament", "Round", "Player 1", "Player 2", "Referee", "Court", "Scheduled Time", "Status"};
        csvWriter.writeNext(header);

        // Write data
        for (MatchDto match : matches) {
            String[] data = {
                    match.getId().toString(),
                    match.getTournamentName(),
                    match.getRound() != null ? match.getRound().toString() : "",
                    match.getPlayer1Name(),
                    match.getPlayer2Name(),
                    match.getRefereeName(),
                    match.getCourtNumber() != null ? match.getCourtNumber().toString() : "",
                    match.getScheduledTime().format(DATE_TIME_FORMATTER),
                    match.getStatus().toString()
            };
            csvWriter.writeNext(data);
        }

        csvWriter.close();
    }
}