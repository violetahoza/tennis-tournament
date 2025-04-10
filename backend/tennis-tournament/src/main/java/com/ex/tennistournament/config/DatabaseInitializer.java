//package com.ex.tennistournament.config;
//
//import com.ex.tennistournament.model.*;
//import com.ex.tennistournament.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class DatabaseInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final TournamentRepository tournamentRepository;
//    private final TournamentRegistrationRepository registrationRepository;
//    private final MatchRepository matchRepository;
//    private final MatchScoreRepository scoreRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) throws Exception {
//        initializeUsers();
//        List<Tournament> tournaments = initializeTournaments();
//        List<User> players = userRepository.findByUserType(User.UserType.PLAYER);
//        List<User> referees = userRepository.findByUserType(User.UserType.REFEREE);
//        initializeRegistrations(tournaments, players);
//        initializeMatches(tournaments, players, referees);
//    }
//
//    private void initializeUsers() {
//        List<User> users = new ArrayList<>();
//
//        // Admin users
//        users.add(User.builder()
//                .username("admin1")
//                .password(passwordEncoder.encode("Admin123"))
//                .email("admin1@tennis.com")
//                .firstName("Admin1")
//                .lastName("User1")
//                .userType(User.UserType.ADMIN)
//                .build());
//
//        users.add(User.builder()
//                .username("admin2")
//                .password(passwordEncoder.encode("Admin123"))
//                .email("admin2@tennis.com")
//                .firstName("John")
//                .lastName("Admin")
//                .userType(User.UserType.ADMIN)
//                .build());
//
//        // Player users
//        users.add(User.builder()
//                .username("roger")
//                .password(passwordEncoder.encode("Player123"))
//                .email("roger@tennis.com")
//                .firstName("Roger")
//                .lastName("Federer")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        users.add(User.builder()
//                .username("novak")
//                .password(passwordEncoder.encode("Player123"))
//                .email("novak@tennis.com")
//                .firstName("Novak")
//                .lastName("Djokovic")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        users.add(User.builder()
//                .username("serena")
//                .password(passwordEncoder.encode("Player123"))
//                .email("serena@tennis.com")
//                .firstName("Serena")
//                .lastName("Williams")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        users.add(User.builder()
//                .username("naomi")
//                .password(passwordEncoder.encode("Player123"))
//                .email("naomi@tennis.com")
//                .firstName("Naomi")
//                .lastName("Osaka")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        users.add(User.builder()
//                .username("rafael")
//                .password(passwordEncoder.encode("Player123"))
//                .email("rafael@tennis.com")
//                .firstName("Rafael")
//                .lastName("Nadal")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        users.add(User.builder()
//                .username("andy")
//                .password(passwordEncoder.encode("Player123"))
//                .email("andy@tennis.com")
//                .firstName("Andy")
//                .lastName("Murray")
//                .userType(User.UserType.PLAYER)
//                .build());
//
//        // Referee users
//        users.add(User.builder()
//                .username("ref1")
//                .password(passwordEncoder.encode("Referee123"))
//                .email("ref1@tennis.com")
//                .firstName("James")
//                .lastName("Referee")
//                .userType(User.UserType.REFEREE)
//                .build());
//
//        users.add(User.builder()
//                .username("ref2")
//                .password(passwordEncoder.encode("Referee123"))
//                .email("ref2@tennis.com")
//                .firstName("Sarah")
//                .lastName("Judge")
//                .userType(User.UserType.REFEREE)
//                .build());
//
//        users.add(User.builder()
//                .username("ref3")
//                .password(passwordEncoder.encode("Referee123"))
//                .email("ref3@tennis.com")
//                .firstName("Michael")
//                .lastName("Umpire")
//                .userType(User.UserType.REFEREE)
//                .build());
//
//        users.add(User.builder()
//                .username("ref4")
//                .password(passwordEncoder.encode("Referee123"))
//                .email("ref4@tennis.com")
//                .firstName("Emma")
//                .lastName("Linewatch")
//                .userType(User.UserType.REFEREE)
//                .build());
//
//        users.add(User.builder()
//                .username("ref5")
//                .password(passwordEncoder.encode("Referee123"))
//                .email("ref5@tennis.com")
//                .firstName("Carlos")
//                .lastName("Official")
//                .userType(User.UserType.REFEREE)
//                .build());
//
//        userRepository.saveAll(users);
//    }
//
//    private List<Tournament> initializeTournaments() {
//        List<Tournament> tournaments = new ArrayList<>();
//
//        // Current year tournaments
//        LocalDate currentDate = LocalDate.now();
//
//        // Upcoming tournament
//        tournaments.add(Tournament.builder()
//                .name("Spring Open Championship")
//                .description("Annual tennis tournament for professional players")
//                .location("New York, USA")
//                .startDate(currentDate.plusMonths(1))
//                .endDate(currentDate.plusMonths(1).plusDays(14))
//                .registrationDeadline(currentDate.plusDays(15))
//                .maxParticipants(32)
//                .build());
//
//        tournaments.add(Tournament.builder()
//                .name("Summer Tennis Masters")
//                .description("Elite tournament with the world's top players")
//                .location("London, UK")
//                .startDate(currentDate.plusMonths(3))
//                .endDate(currentDate.plusMonths(3).plusDays(10))
//                .registrationDeadline(currentDate.plusMonths(2))
//                .maxParticipants(16)
//                .build());
//
//        // Tournament in progress
//        tournaments.add(Tournament.builder()
//                .name("Current Championship")
//                .description("Ongoing competitive tournament")
//                .location("Paris, France")
//                .startDate(currentDate.minusDays(5))
//                .endDate(currentDate.plusDays(5))
//                .registrationDeadline(currentDate.minusDays(15))
//                .maxParticipants(24)
//                .build());
//
//        // Past tournaments
//        tournaments.add(Tournament.builder()
//                .name("Winter Tennis Cup")
//                .description("Indoor tournament during winter season")
//                .location("Melbourne, Australia")
//                .startDate(currentDate.minusMonths(2))
//                .endDate(currentDate.minusMonths(2).plusDays(7))
//                .registrationDeadline(currentDate.minusMonths(3))
//                .maxParticipants(16)
//                .build());
//
//        tournaments.add(Tournament.builder()
//                .name("Last Season Championship")
//                .description("Last year's major championship")
//                .location("Madrid, Spain")
//                .startDate(currentDate.minusMonths(6))
//                .endDate(currentDate.minusMonths(6).plusDays(14))
//                .registrationDeadline(currentDate.minusMonths(7))
//                .maxParticipants(32)
//                .build());
//
//        return tournamentRepository.saveAll(tournaments);
//    }
//
//    private void initializeRegistrations(List<Tournament> tournaments, List<User> players) {
//        List<TournamentRegistration> registrations = new ArrayList<>();
//
//        // Get upcoming tournaments where registration is still open
//        Tournament upcomingTournament1 = tournaments.get(0); // Spring Open
//        Tournament upcomingTournament2 = tournaments.get(1); // Summer Masters
//
//        // Register players for the first upcoming tournament
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(0))
//                .tournament(upcomingTournament1)
//                .registrationDate(LocalDateTime.now().minusDays(5))
//                .status(TournamentRegistration.RegistrationStatus.APPROVED)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(1))
//                .tournament(upcomingTournament1)
//                .registrationDate(LocalDateTime.now().minusDays(4))
//                .status(TournamentRegistration.RegistrationStatus.APPROVED)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(2))
//                .tournament(upcomingTournament1)
//                .registrationDate(LocalDateTime.now().minusDays(3))
//                .status(TournamentRegistration.RegistrationStatus.APPROVED)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(3))
//                .tournament(upcomingTournament1)
//                .registrationDate(LocalDateTime.now().minusDays(2))
//                .status(TournamentRegistration.RegistrationStatus.PENDING)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(4))
//                .tournament(upcomingTournament1)
//                .registrationDate(LocalDateTime.now().minusDays(1))
//                .status(TournamentRegistration.RegistrationStatus.PENDING)
//                .build());
//
//        // Register players for the second upcoming tournament
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(0))
//                .tournament(upcomingTournament2)
//                .registrationDate(LocalDateTime.now().minusDays(10))
//                .status(TournamentRegistration.RegistrationStatus.APPROVED)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(2))
//                .tournament(upcomingTournament2)
//                .registrationDate(LocalDateTime.now().minusDays(9))
//                .status(TournamentRegistration.RegistrationStatus.APPROVED)
//                .build());
//
//        registrations.add(TournamentRegistration.builder()
//                .player(players.get(5))
//                .tournament(upcomingTournament2)
//                .registrationDate(LocalDateTime.now().minusDays(8))
//                .status(TournamentRegistration.RegistrationStatus.REJECTED)
//                .build());
//
//        registrationRepository.saveAll(registrations);
//    }
//
//    private void initializeMatches(List<Tournament> tournaments, List<User> players, List<User> referees) {
//        List<Match> matches = new ArrayList<>();
//
//        // Current tournament matches
//        Tournament currentTournament = tournaments.get(2);
//
//        // Match 1: Completed match
//        Match match1 = Match.builder()
//                .tournament(currentTournament)
//                .player1(players.get(0))
//                .player2(players.get(1))
//                .referee(referees.get(0))
//                .courtNumber(1)
//                .scheduledTime(LocalDateTime.now().minusDays(3))
//                .status(Match.MatchStatus.COMPLETED)
//                .round(Match.Round.ROUND_1)
//                .build();
//
//        matches.add(match1);
//
//        // Match 2: In Progress
//        Match match2 = Match.builder()
//                .tournament(currentTournament)
//                .player1(players.get(2))
//                .player2(players.get(3))
//                .referee(referees.get(1))
//                .courtNumber(2)
//                .scheduledTime(LocalDateTime.now().minusDays(1))
//                .status(Match.MatchStatus.IN_PROGRESS)
//                .round(Match.Round.ROUND_1)
//                .build();
//
//        matches.add(match2);
//
//        // Match 3: Scheduled match
//        Match match3 = Match.builder()
//                .tournament(currentTournament)
//                .player1(players.get(0)) // Winner of match 1
//                .player2(players.get(4))
//                .referee(referees.get(2))
//                .courtNumber(1)
//                .scheduledTime(LocalDateTime.now().plusDays(1))
//                .status(Match.MatchStatus.SCHEDULED)
//                .round(Match.Round.ROUND_2)
//                .build();
//
//        matches.add(match3);
//
//        // Match 4: Scheduled match
//        Match match4 = Match.builder()
//                .tournament(currentTournament)
//                .player1(players.get(5))
//                .player2(players.get(1)) // Loser from match 1
//                .referee(referees.get(3))
//                .courtNumber(3)
//                .scheduledTime(LocalDateTime.now().plusDays(2))
//                .status(Match.MatchStatus.SCHEDULED)
//                .round(Match.Round.ROUND_2)
//                .build();
//
//        matches.add(match4);
//
//        // Match 5: Cancelled match
//        Match match5 = Match.builder()
//                .tournament(currentTournament)
//                .player1(players.get(4))
//                .player2(players.get(5))
//                .referee(referees.get(0))
//                .courtNumber(4)
//                .scheduledTime(LocalDateTime.now().minusDays(2))
//                .status(Match.MatchStatus.CANCELLED)
//                .round(Match.Round.ROUND_1)
//                .build();
//
//        matches.add(match5);
//
//        // Match 6: Past tournament match
//        Match match6 = Match.builder()
//                .tournament(tournaments.get(3)) // Winter Tennis Cup
//                .player1(players.get(0))
//                .player2(players.get(3))
//                .referee(referees.get(1))
//                .courtNumber(1)
//                .scheduledTime(LocalDateTime.now().minusMonths(2).plusDays(2))
//                .status(Match.MatchStatus.COMPLETED)
//                .round(Match.Round.FINAL)
//                .build();
//
//        matches.add(match6);
//
//        matchRepository.saveAll(matches);
//
//        // Add scores for completed matches
//        List<MatchScore> scores = new ArrayList<>();
//
//        // Scores for match 1
//        scores.add(MatchScore.builder()
//                .match(match1)
//                .setNumber(1)
//                .player1Score(6)
//                .player2Score(4)
//                .build());
//
//        scores.add(MatchScore.builder()
//                .match(match1)
//                .setNumber(2)
//                .player1Score(6)
//                .player2Score(3)
//                .build());
//
//        // Scores for match 2 (in progress)
//        scores.add(MatchScore.builder()
//                .match(match2)
//                .setNumber(1)
//                .player1Score(7)
//                .player2Score(5)
//                .build());
//
//        scores.add(MatchScore.builder()
//                .match(match2)
//                .setNumber(2)
//                .player1Score(4)
//                .player2Score(6)
//                .build());
//
//        // Scores for match 6 (past tournament)
//        scores.add(MatchScore.builder()
//                .match(match6)
//                .setNumber(1)
//                .player1Score(6)
//                .player2Score(2)
//                .build());
//
//        scores.add(MatchScore.builder()
//                .match(match6)
//                .setNumber(2)
//                .player1Score(6)
//                .player2Score(4)
//                .build());
//
//        scoreRepository.saveAll(scores);
//    }
//}