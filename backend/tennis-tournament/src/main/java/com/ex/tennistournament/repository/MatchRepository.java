package com.ex.tennistournament.repository;

import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing Match entities.
 * This interface extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournament(Tournament tournament);
    List<Match> findByPlayer1OrPlayer2(User player1, User player2);
    List<Match> findByReferee(User referee);
    List<Match> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Match> findByTournamentAndStatus(Tournament tournament, Match.MatchStatus status);
    List<Match> findByTournamentId(Long tournamentId);
}