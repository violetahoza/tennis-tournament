package com.ex.tennistournament.repository;

import com.ex.tennistournament.model.Tournament;
import com.ex.tennistournament.model.TournamentRegistration;
import com.ex.tennistournament.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing TournamentRegistration entities.
 * This interface extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
    List<TournamentRegistration> findByPlayer(User player);
    List<TournamentRegistration> findByTournament(Tournament tournament);
    Optional<TournamentRegistration> findByPlayerAndTournament(User player, Tournament tournament);

    @Query("SELECT COUNT(tr) FROM TournamentRegistration tr WHERE tr.tournament.id = ?1 AND tr.status = 'APPROVED'")
    long countApprovedRegistrationsByTournamentId(Long tournamentId);

    @Query("SELECT tr FROM TournamentRegistration tr WHERE tr.tournament.id = ?1 AND tr.status = ?2 ORDER BY tr.registrationDate ASC")
    List<TournamentRegistration> findTopNByTournamentIdAndStatusOrderByRegistrationDateAsc(
            Long tournamentId,
            TournamentRegistration.RegistrationStatus status,
            int limit);

//    @Transactional
//    @Modifying
//    @Query("DELETE FROM TournamentRegistration tr WHERE tr.tournament.id = ?1")
//    void deleteByTournamentId(Long tournamentId);
}
