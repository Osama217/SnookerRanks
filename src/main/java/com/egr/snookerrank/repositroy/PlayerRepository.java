package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.beans.AnnualWinLoss;
import com.egr.snookerrank.dto.PlayerMatchTournamentDTO;
import com.egr.snookerrank.dto.PlayerPrizeTournamentDTO;
import com.egr.snookerrank.dto.PlayerTournamentDTO;
import com.egr.snookerrank.dto.TournamentEventDTO;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.beans.PlayerPrizeStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {

    Player findByPlayerKey(Integer playerKey);


    @Query(value = "SELECT TOP(:count) player_key,player_name,country_name,fdi FROM player WHERE fdi_matches > 20 ORDER BY fdi DESC", nativeQuery = true)
    List<Object[]> findTopPlayers(@Param("count") int count);

    @Query(value = """
        SELECT TOP(:topCount) 
            p.player_key AS playerKey, 
            p.player_name AS playerName, 
            p.country_name AS countryName, 
            p.age AS age, 
            p.dob AS dob, 
            p.is_woman AS isWoman, 
            ROUND(SUM(pp.prize_money * e.conversion_rate), 0) AS sumPrizeMoney, 
            COUNT(*) AS totalEvents, 
            MAX(in_progress) AS inProgress
        FROM player p
        JOIN player_prize pp ON p.player_key = pp.player_key
        LEFT JOIN player_pro_card ppc1 ON p.player_key = ppc1.player_key AND ppc1.year = :effectiveYear
        LEFT JOIN player_pro_card ppc2 ON p.player_key = ppc2.player_key AND ppc2.year = (:effectiveYear - 1)
        JOIN event e ON pp.event_key = e.event_key
        JOIN tournament t ON e.tournament_key = t.tournament_key
        WHERE e.event_date >= :dateFrom
          AND e.event_date <= :dateTo
          AND e.event_category NOT IN ('U', '0')
          AND pp.round_no <> -501
          AND (:playerKeys IS NULL OR p.player_key NOT IN (:playerKeys))
        GROUP BY p.player_key, p.player_name, p.country_name, p.age, p.dob, p.is_woman
        HAVING SUM(pp.prize_money) > 0
        ORDER BY sumPrizeMoney DESC
        """, nativeQuery = true)
    List<Object[]> findOrderOfMerit(
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo,
            @Param("topCount") int topCount,
            @Param("effectiveYear") int effectiveYear,
            @Param("playerKeys") List<Integer> playerKeys
    );

    @Query(value = "SELECT rank_text_key,rank_name FROM rank_text WHERE is_ranking=0 ORDER BY order_num", nativeQuery = true)
    List<Object[]> fetchRanks();

    @Query("SELECT p FROM Player p WHERE LOWER(p.playerName) LIKE LOWER(CONCAT('%', :searchString, '%')) ORDER BY p.fdi DESC")
    List<Player> findByPlayerNameContainingOrderByFdiDesc(@Param("searchString") String searchString);

    @Query(value = "SELECT YEAR(e.event_date) AS yearActive, " +
            "SUM(pp.prize_money) AS totalPrizeMoney, " +
            "COUNT(CASE WHEN pp.round_no = 21 THEN 1 END) AS titlesWon, " +
            "SUM(pp.prize_money) / COUNT(*) AS prizePerEvent " +
            "FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "WHERE pp.player_key = :playerKey " +
            "AND (:rankingOnly != 'Y' OR (e.event_category <> 'U' AND e.event_category <> '0')) " +
            "GROUP BY YEAR(e.event_date) " +
            "ORDER BY yearActive",
            nativeQuery = true)
    List<PlayerPrizeStats> findPlayerPrizeStatistics(@Param("playerKey") Integer playerKey,
                                                     @Param("rankingOnly") String rankingOnly);

    @Query(value = "SELECT t.tournament_key AS tournamentKey, " +
            "t.tournament_name AS tournamentName, " +
            "t.prestige AS prestige, " +
            "(SELECT round_no FROM round WHERE order_num = MAX(ppr.order_num)) AS roundNo, " +
            "(SELECT round_name FROM round WHERE order_num = MAX(ppr.order_num)) AS roundName " +
            "FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "JOIN round ppr ON pp.round_no = ppr.round_no " +
            "WHERE pp.player_key = :playerKey AND t.prestige <> 0 " +
            "GROUP BY t.tournament_key, t.tournament_name, t.prestige " +
            "ORDER BY t.prestige DESC, t.tournament_name",
            nativeQuery = true)
    List<PlayerTournamentDTO> findTournamentsByPlayer(@Param("playerKey") Integer playerKey);

    @Query(value = "SELECT Year(e.event_date) " +
            "FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "WHERE pp.player_key = :playerKey " +
            "AND t.tournament_key = :tournamentKey " +
            "AND pp.round_no = :roundNo " +
            "ORDER BY e.event_date",
            nativeQuery = true)
    List<Integer> findEventDates(@Param("playerKey") Integer playerKey,
                                       @Param("tournamentKey") Integer tournamentKey,
                                       @Param("roundNo") Integer roundNo);

    @Query(value = "SELECT t.tournament_key AS tournamentKey, " +
            "t.tournament_name AS tournamentName, " +
            "Year(e.event_date) AS eventDate " +
            "FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "WHERE pp.player_key = :playerKey " +
            "AND t.prestige = 0 " +
            "AND pp.round_no = 21 " +
            "ORDER BY t.tournament_name, e.event_date",
            nativeQuery = true)
    List<TournamentEventDTO> findTournamentEvents(@Param("playerKey") Integer playerKey);

    @Query(value = "SELECT YEAR(m.match_date) AS year, " +
            "SUM(CASE WHEN m.winner_key = :playerKey THEN 1 ELSE 0 END) AS wins, " +
            "SUM(CASE WHEN m.loser_key = :playerKey THEN 1 ELSE 0 END) AS losses, " +
            "SUM(CASE WHEN m.winner_key = :playerKey THEN m.winner_score ELSE m.loser_score END) AS legsWon, " +
            "SUM(CASE WHEN m.winner_key = :playerKey THEN m.loser_score ELSE m.winner_score END) AS legsLost, " +
            "COUNT(*) AS matches " +
            "FROM match m " +
            "WHERE (m.winner_key = :playerKey OR m.loser_key = :playerKey) " +
            "AND (m.winner_score = 0 OR m.winner_score > m.loser_score) " +
            "GROUP BY YEAR(m.match_date) " +
            "ORDER BY YEAR(m.match_date)",
            nativeQuery = true)
    List<AnnualWinLoss> getAnnualWinLossRecords(@Param("playerKey") Integer playerKey);


    @Query(value = "SELECT " +
            "t.tournament_key AS tournamentKey, " +
            "e.event_key AS eventKey, " +
            "t.tournament_name AS tournamentName, " +
            "e.tournament_no AS tournamentNo, " +
            "CAST(e.event_date As DATE)AS eventDate, " +
            "e.event_category AS eventCategory, " +
            "pp.prize_money AS prizeMoney, " +
            "r.round_name AS roundName, " +
            "full_prizes AS fullPrizes, " +
            "e.currency_code AS currencyCode " +
            "FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "WHERE pp.player_key = :playerKey " +
            "ORDER BY e.event_date DESC, e.tournament_no DESC",
            nativeQuery = true)
    List<PlayerPrizeTournamentDTO> findPlayerPrizes(@Param("playerKey") Integer playerKey);


    @Query(value = "SELECT " +
            "t.tournament_key AS tournamentKey, " +
            "e.event_key AS eventKey, " +
            "t.tournament_name AS tournamentName, " +
            "e.tournament_no AS tournamentNo, " +
            "CAST(m.match_date AS DATE)AS matchDate, " +
            "e.event_category AS eventCategory, " +
            "r.round_name AS roundName, " +
            "m.winner_key AS winnerKey, " +
            "m.loser_key AS loserKey, " +
            "w.player_name AS winnerName, " +
            "l.player_name AS loserName, " +
            "m.winner_score AS winnerScore, " +
            "m.loser_score AS loserScore, " +
            "m.is_bye AS isBye " +
            "FROM match m " +
            "JOIN event e ON m.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "JOIN round r ON m.round_no = r.round_no " +
            "JOIN player w ON m.winner_key = w.player_key " +
            "JOIN player l ON m.loser_key = l.player_key " +
            "WHERE (m.winner_key = :playerKey OR m.loser_key = :playerKey) " +
            "ORDER BY m.match_date DESC, e.tournament_no DESC, e.event_key DESC, r.order_num DESC, m.match_key DESC",
            nativeQuery = true)
    List<PlayerMatchTournamentDTO> findMatchesByPlayer(@Param("playerKey") Integer playerKey);

}
