package com.ex.tennistournament.repository;

import com.ex.tennistournament.model.Match;
import com.ex.tennistournament.model.MatchScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing MatchScore entities.
 * This interface extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface MatchScoreRepository extends JpaRepository<MatchScore, Long> {
    List<MatchScore> findByMatch(Match match);
    List<MatchScore> findByMatchOrderBySetNumber(Match match);
}