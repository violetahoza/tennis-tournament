package com.ex.tennistournament.repository;

import com.ex.tennistournament.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByStartDateAfter(LocalDate date);
    List<Tournament> findByRegistrationDeadlineAfter(LocalDate date);
}