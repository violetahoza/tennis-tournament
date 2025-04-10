package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchScoreService {

    private final MatchScoreRepository matchScoreRepository;
    private final MatchRepository matchRepository;

    public List<MatchScoreDto> getScoresByMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        return matchScoreRepository.findByMatchOrderBySetNumber(match).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MatchScoreDto getScoreById(Long id) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));
        return mapToDto(score);
    }

    @Transactional
    public MatchScoreDto createScore(MatchScoreDto scoreDto) {
        Match match = matchRepository.findById(scoreDto.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + scoreDto.getMatchId()));

        // Verify match is in progress or scheduled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add scores to completed or cancelled matches");
        }

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can update match scores");
        }

        // Check if set number already exists
        matchScoreRepository.findByMatchOrderBySetNumber(match).stream()
                .filter(existingScore -> existingScore.getSetNumber().equals(scoreDto.getSetNumber()))
                .findFirst()
                .ifPresent(existingScore -> {
                    throw new IllegalArgumentException("Score for set " + scoreDto.getSetNumber() + " already exists");
                });

        // Create new score
        MatchScore score = MatchScore.builder()
                .match(match)
                .setNumber(scoreDto.getSetNumber())
                .player1Score(scoreDto.getPlayer1Score())
                .player2Score(scoreDto.getPlayer2Score())
                .build();

        // Update match status to IN_PROGRESS if it was SCHEDULED
        if (match.getStatus() == Match.MatchStatus.SCHEDULED) {
            match.setStatus(Match.MatchStatus.IN_PROGRESS);
            matchRepository.save(match);
        }

        MatchScore savedScore = matchScoreRepository.save(score);
        return mapToDto(savedScore);
    }

    @Transactional
    public MatchScoreDto updateScore(Long id, MatchScoreDto scoreDto) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));

        Match match = score.getMatch();

        // Verify match is not completed or cancelled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update scores of completed or cancelled matches");
        }

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can update match scores");
        }

        score.setPlayer1Score(scoreDto.getPlayer1Score());
        score.setPlayer2Score(scoreDto.getPlayer2Score());

        MatchScore updatedScore = matchScoreRepository.save(score);
        return mapToDto(updatedScore);
    }

    @Transactional
    public void deleteScore(Long id) {
        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score not found with id: " + id));

        Match match = score.getMatch();

        // Verify match is not completed or cancelled
        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot delete scores of completed or cancelled matches");
        }

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can delete match scores");
        }

        matchScoreRepository.deleteById(id);
    }

    @Transactional
    public void completeMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // Verify current user is the referee of the match
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (currentUser.getUserType() == User.UserType.REFEREE && !match.getReferee().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the assigned referee can complete the match");
        }

        // Check if there are any scores recorded
        List<MatchScore> scores = matchScoreRepository.findByMatch(match);
        if (scores.isEmpty()) {
            throw new IllegalStateException("Cannot complete match without any scores");
        }

        // Update match status to COMPLETED
        match.setStatus(Match.MatchStatus.COMPLETED);
        matchRepository.save(match);
    }

    private MatchScoreDto mapToDto(MatchScore score) {
        return MatchScoreDto.builder()
                .id(score.getId())
                .matchId(score.getMatch().getId())
                .setNumber(score.getSetNumber())
                .player1Score(score.getPlayer1Score())
                .player2Score(score.getPlayer2Score())
                .build();
    }
}