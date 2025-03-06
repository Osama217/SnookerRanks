package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.beans.PlayerPrizeStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}
