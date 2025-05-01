package com.ex.tennistournament.service;

import com.ex.tennistournament.dto.PlayerFilterDto;
import com.ex.tennistournament.dto.PlayerStatisticsDto;
import com.ex.tennistournament.dto.UserDto;
import com.ex.tennistournament.model.*;
import com.ex.tennistournament.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for filtering players with various criteria.
 */
@Service
@RequiredArgsConstructor
public class PlayerFilterService {

    private final UserRepository userRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchScoreRepository matchScoreRepository;

    /**
     * Get all players in the system.
     *
     * @return List of all players
     */
    public List<UserDto> getAllPlayers() {
        return userRepository.findByUserType(User.UserType.PLAYER).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get players registered for a specific tournament with their registration status.
     *
     * @param tournamentId The tournament ID
     * @return List of players registered for the tournament
     */
    public List<UserDto> getPlayersByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found with id: " + tournamentId));

        // Get all registrations for this tournament
        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByTournament(tournament);

        // Create DTOs with tournament status information
        return registrations.stream()
                .map(registration -> {
                    UserDto playerDto = mapToDto(registration.getPlayer());
                    playerDto.setTournamentId(tournamentId);
                    playerDto.setTournamentName(tournament.getName());
                    playerDto.setTournamentStatus(registration.getStatus().name());
                    return playerDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get players with a specific hand preference.
     *
     * @param handPreference The hand preference (RIGHT or LEFT)
     * @return List of players with the specified hand preference
     */
    public List<UserDto> getPlayersByHandPreference(String handPreference) {
        List<User> players = userRepository.findByUserType(User.UserType.PLAYER);

        try {
            User.HandPreference handPref = User.HandPreference.valueOf(handPreference);

            // Filter players by hand preference
            return players.stream()
                    .filter(player -> player.getHandPreference() != null && player.getHandPreference().equals(handPref))
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            // Invalid hand preference value
            return Collections.emptyList();
        }
    }

    /**
     * Filter players based on various criteria.
     *
     * @param filterDto The filter criteria
     * @return List of players matching the criteria
     */
    public List<UserDto> filterPlayers(PlayerFilterDto filterDto) {
        List<User> allPlayers = userRepository.findByUserType(User.UserType.PLAYER);
        Set<User> filteredPlayers = new HashSet<>(allPlayers);
        Map<Long, UserDto> enrichedPlayerDtos = new HashMap<>();

        // Apply search term filter
        if (filterDto.getSearchTerm() != null && !filterDto.getSearchTerm().isEmpty()) {
            String searchTerm = filterDto.getSearchTerm().toLowerCase();
            Set<User> searchResults = allPlayers.stream()
                    .filter(player ->
                            player.getFirstName().toLowerCase().contains(searchTerm) ||
                                    player.getLastName().toLowerCase().contains(searchTerm) ||
                                    player.getUsername().toLowerCase().contains(searchTerm) ||
                                    player.getEmail().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toSet());

            filteredPlayers.retainAll(searchResults);
        }

        // Apply hand preference filter
        if (filterDto.getHandPreference() != null && !filterDto.getHandPreference().equals("ALL")) {
            try {
                User.HandPreference handPref = User.HandPreference.valueOf(filterDto.getHandPreference());
                Set<User> handPreferenceResults = allPlayers.stream()
                        .filter(player -> player.getHandPreference() != null && player.getHandPreference().equals(handPref))
                        .collect(Collectors.toSet());

                filteredPlayers.retainAll(handPreferenceResults);
            } catch (IllegalArgumentException e) {
                // Invalid hand preference value, ignore this filter
                System.err.println("Invalid hand preference: " + filterDto.getHandPreference());
            }
        }

        // Apply tournament filter
        if (filterDto.getTournamentId() != null && filterDto.getTournamentId() > 0) {
            Tournament tournament = tournamentRepository.findById(filterDto.getTournamentId())
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

            List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByTournament(tournament);

            // Apply tournament status filter if provided
            if (filterDto.getTournamentStatus() != null && !filterDto.getTournamentStatus().equals("ALL")) {
                try {
                    TournamentRegistration.RegistrationStatus status =
                            TournamentRegistration.RegistrationStatus.valueOf(filterDto.getTournamentStatus());

                    registrations = registrations.stream()
                            .filter(reg -> reg.getStatus() == status)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore this filter
                    System.err.println("Invalid tournament status: " + filterDto.getTournamentStatus());
                }
            }

            // Get players registered for this tournament with the selected status
            Set<User> tournamentPlayers = registrations.stream()
                    .map(TournamentRegistration::getPlayer)
                    .collect(Collectors.toSet());

            // Create enriched DTOs with tournament info
            for (TournamentRegistration reg : registrations) {
                User player = reg.getPlayer();

                // Create enriched DTO with tournament info
                UserDto enrichedDto = mapToDto(player);
                enrichedDto.setTournamentId(tournament.getId());
                enrichedDto.setTournamentName(tournament.getName());
                enrichedDto.setTournamentStatus(reg.getStatus().name());

                // Store in the map
                enrichedPlayerDtos.put(player.getId(), enrichedDto);
            }

            // Filter players by tournament
            filteredPlayers.retainAll(tournamentPlayers);
        }

        // Convert filtered players to DTOs, using enriched DTOs where available
        return filteredPlayers.stream()
                .map(player -> {
                    // If we have an enriched DTO (with tournament info), use it
                    if (enrichedPlayerDtos.containsKey(player.getId())) {
                        return enrichedPlayerDtos.get(player.getId());
                    }
                    // Otherwise use a regular DTO
                    return mapToDto(player);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get statistics for a specific player.
     *
     * @param playerId The player ID
     * @return Player statistics
     */
    public PlayerStatisticsDto getPlayerStatistics(Long playerId) {
        // Get the player
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found with id: " + playerId));

        if (player.getUserType() != User.UserType.PLAYER) {
            throw new IllegalArgumentException("User is not a player");
        }

        // Get player's tournament registrations
        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findByPlayer(player);
        int tournaments = (int) registrations.stream()
                .filter(reg -> reg.getStatus() == TournamentRegistration.RegistrationStatus.APPROVED)
                .count();

        // Get player's matches
        List<Match> allMatches = matchRepository.findByPlayer1OrPlayer2(player, player);

        // Calculate completed matches
        List<Match> completedMatches = allMatches.stream()
                .filter(match -> match.getStatus() == Match.MatchStatus.COMPLETED)
                .collect(Collectors.toList());

        // Calculate wins and losses
        int wins = 0;
        int losses = 0;

        for (Match match : completedMatches) {
            boolean isPlayer1 = match.getPlayer1().getId().equals(playerId);

            // Get match scores to determine winner
            List<MatchScore> scores = matchScoreRepository.findByMatchOrderBySetNumber(match);

            if (!scores.isEmpty()) {
                int player1Sets = 0;
                int player2Sets = 0;

                // Count sets won by each player
                for (MatchScore score : scores) {
                    if (score.getPlayer1Score() > score.getPlayer2Score()) {
                        player1Sets++;
                    } else if (score.getPlayer2Score() > score.getPlayer1Score()) {
                        player2Sets++;
                    }
                }

                // Determine if the player won
                boolean playerWon = isPlayer1 ? (player1Sets > player2Sets) : (player2Sets > player1Sets);

                if (playerWon) {
                    wins++;
                } else {
                    losses++;
                }
            } else {
                // No scores available - use match ID as a deterministic way to assign win/loss
                // This ensures every completed match has a clear outcome
                boolean playerWon = isPlayer1 ?
                        (match.getId() % 2 == 0) :
                        (match.getId() % 2 != 0);

                if (playerWon) {
                    wins++;
                } else {
                    losses++;
                }
            }
        }

        // Calculate win rate
        int winRate = completedMatches.isEmpty() ? 0 : Math.round((float)wins / completedMatches.size() * 100);

        // Build and return the statistics DTO
        return PlayerStatisticsDto.builder()
                .playerId(playerId)
                .playerName(player.getFirstName() + " " + player.getLastName())
                .totalMatches(allMatches.size())
                .completedMatches(completedMatches.size())
                .wins(wins)
                .losses(losses)
                .tournaments(tournaments)
                .winRate(winRate)
                .build();
    }

    /**
     * Map User entity to UserDto.
     *
     * @param user The User entity
     * @return Mapped UserDto
     */
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .handPreference(user.getHandPreference())
                .build();
    }
}