package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.MatchDto;
import com.ex.tennistournament.dto.MatchScoreDto;
import com.ex.tennistournament.dto.MatchSummaryDto;
import com.ex.tennistournament.exception.ResourceNotFoundException;
import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.User;
import com.ex.tennistournament.repository.MatchRepository;
import com.ex.tennistournament.repository.MatchScoreRepository;
import com.ex.tennistournament.repository.TournamentRepository;
import com.ex.tennistournament.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    public List<MatchDto> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return matchRepository.findByTournament(tournament).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByPlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return matchRepository.findByPlayer1OrPlayer2(player, player).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MatchDto> getMatchesByReferee(Long refereeId) {
        User referee = userRepository.findById(refereeId)
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + refereeId));

        return matchRepository.findByReferee(referee).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MatchDto getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return mapToDto(match);
    }

    public MatchSummaryDto getMatchSummary(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));

        List<MatchScore> scores = matchScoreRepository.findByMatchOrderBySetNumber(match);

        String winnerName = determineWinner(scores, match);

        return MatchSummaryDto.builder()
                .matchId(match.getId())
                .tournamentName(match.getTournament().getName())
                .player1Name(match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName())
                .player2Name(match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName())
                .refereeName(match.getReferee().getFirstName() + " " + match.getReferee().getLastName())
                .scheduledTime(match.getScheduledTime())
                .status(match.getStatus())
                .round(match.getRound())
                .scores(scores.stream().map(this::mapScoreToDto).collect(Collectors.toList()))
                .winner(winnerName)
                .build();
    }

    @Transactional
    public MatchDto createMatch(MatchDto matchDto) {
        Tournament tournament = tournamentRepository.findById(matchDto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + matchDto.getTournamentId()));

        User player1 = userRepository.findById(matchDto.getPlayer1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 1 not found with id: " + matchDto.getPlayer1Id()));

        User player2 = userRepository.findById(matchDto.getPlayer2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 2 not found with id: " + matchDto.getPlayer2Id()));

        User referee = userRepository.findById(matchDto.getRefereeId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

        // Validate user types
        if (player1.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("Player 1 must be a player");
        }

        if (player2.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("Player 2 must be a player");
        }

        if (referee.getUserType() != User.UserType.REFEREE) {
            throw new IllegalArgumentException("Referee must be a referee");
        }

        Match match = Match.builder()
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .referee(referee)
                .courtNumber(matchDto.getCourtNumber())
                .scheduledTime(matchDto.getScheduledTime())
                .status(Match.MatchStatus.SCHEDULED)
                .round(matchDto.getRound())
                .build();

        Match savedMatch = matchRepository.save(match);
        return mapToDto(savedMatch);
    }

    @Transactional
    public MatchDto updateMatch(Long id, MatchDto matchDto) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));

        Tournament tournament = tournamentRepository.findById(matchDto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + matchDto.getTournamentId()));

        User player1 = userRepository.findById(matchDto.getPlayer1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 1 not found with id: " + matchDto.getPlayer1Id()));

        User player2 = userRepository.findById(matchDto.getPlayer2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Player 2 not found with id: " + matchDto.getPlayer2Id()));

        User referee = userRepository.findById(matchDto.getRefereeId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee not found with id: " + matchDto.getRefereeId()));

        // Validate user types
        if (player1.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("Player 1 must be a player");
        }

        if (player2.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("Player 2 must be a player");
        }

        if (referee.getUserType() != User.UserType.REFEREE) {
            throw new IllegalArgumentException("Referee must be a referee");
        }

        match.setTournament(tournament);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setReferee(referee);
        match.setCourtNumber(matchDto.getCourtNumber());
        match.setScheduledTime(matchDto.getScheduledTime());
        match.setStatus(matchDto.getStatus());
        match.setRound(matchDto.getRound());

        Match updatedMatch = matchRepository.save(match);
        return mapToDto(updatedMatch);
    }

    @Transactional
    public void deleteMatch(Long id) {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }
        matchRepository.deleteById(id);
    }

    private MatchDto mapToDto(Match match) {
        return MatchDto.builder()
                .id(match.getId())
                .tournamentId(match.getTournament().getId())
                .tournamentName(match.getTournament().getName())
                .player1Id(match.getPlayer1().getId())
                .player1Name(match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName())
                .player2Id(match.getPlayer2().getId())
                .player2Name(match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName())
                .refereeId(match.getReferee().getId())
                .refereeName(match.getReferee().getFirstName() + " " + match.getReferee().getLastName())
                .courtNumber(match.getCourtNumber())
                .scheduledTime(match.getScheduledTime())
                .status(match.getStatus())
                .round(match.getRound())
                .build();
    }

    private MatchScoreDto mapScoreToDto(MatchScore score) {
        return MatchScoreDto.builder()
                .id(score.getId())
                .matchId(score.getMatch().getId())
                .setNumber(score.getSetNumber())
                .player1Score(score.getPlayer1Score())
                .player2Score(score.getPlayer2Score())
                .build();
    }

    private String determineWinner(List<MatchScore> scores, Match match) {
        if (match.getStatus() != Match.MatchStatus.COMPLETED || scores.isEmpty()) {
            return "Match not completed";
        }

        int player1Sets = 0;
        int player2Sets = 0;

        for (MatchScore score : scores) {
            if (score.getPlayer1Score() > score.getPlayer2Score()) {
                player1Sets++;
            } else if (score.getPlayer2Score() > score.getPlayer1Score()) {
                player2Sets++;
            }
        }

        if (player1Sets > player2Sets) {
            return match.getPlayer1().getFirstName() + " " + match.getPlayer1().getLastName();
        } else if (player2Sets > player1Sets) {
            return match.getPlayer2().getFirstName() + " " + match.getPlayer2().getLastName();
        } else {
            return "Tie";
        }
    }
}
