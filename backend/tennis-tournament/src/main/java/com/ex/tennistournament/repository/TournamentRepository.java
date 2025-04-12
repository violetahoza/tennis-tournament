package com.ex.tennistournament.repository;

import com.ex.tennistournament.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for managing Tournament entities.
 * This interface extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByStartDateAfter(LocalDate date);
    List<Tournament> findByRegistrationDeadlineAfter(LocalDate date);
}