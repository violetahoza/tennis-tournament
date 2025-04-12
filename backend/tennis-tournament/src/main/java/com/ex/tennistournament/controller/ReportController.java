package com.ex.tennistournament.controller;

import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.service.MatchService;
import com.ex.tennistournament.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for generating and exporting match reports.
 * Provides endpoints for downloading match data in various formats.
 *
 * Security:
 * - All endpoints require ADMIN role
 * - Uses @PreAuthorize for role-based authorization
 *
 * Features:
 * - CSV export of match data
 * - TXT export of match data
 *
 * Integration:
 * - Works with ReportService for report generation
 * - Uses MatchService for data retrieval
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MatchService matchService;

    /**
     * Exports match data as CSV file.
     * Supports optional filtering by tournament ID.
     *
     * Security:
     * - Requires ADMIN role
     *
     * @param tournamentId Optional tournament ID for filtering matches
     * @return ResponseEntity containing CSV content with appropriate headers
     * @throws IOException if CSV generation fails
     */
    @GetMapping("/matches/csv")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportMatchesAsCsv(@RequestParam(required = false) Long tournamentId) throws IOException {
        List<MatchDto> matches;
        if (tournamentId != null) {
            matches = matchService.getMatchesByTournament(tournamentId);
        } else {
            matches = matchService.getAllMatches();
        }

        String csvContent = reportService.generateMatchesCSV(matches);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "matches.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }

    /**
     * Exports match data as TXT file.
     * Supports optional filtering by tournament ID.
     *
     * Security:
     * - Requires ADMIN role
     *
     * @param tournamentId Optional tournament ID for filtering matches
     * @return ResponseEntity containing TXT content with appropriate headers
     */
    @GetMapping("/matches/txt")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportMatchesAsTxt(@RequestParam(required = false) Long tournamentId) {
        List<MatchDto> matches;
        if (tournamentId != null) {
            matches = matchService.getMatchesByTournament(tournamentId);
        } else {
            matches = matchService.getAllMatches();
        }

        String txtContent = reportService.generateMatchesTXT(matches);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "matches.txt");

        return ResponseEntity.ok()
                .headers(headers)
                .body(txtContent);
    }
}