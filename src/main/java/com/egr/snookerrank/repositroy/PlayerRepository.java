package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.beans.AnnualWinLoss;
import com.egr.snookerrank.beans.MatchResults;
import com.egr.snookerrank.beans.PrizeFund;
import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.EventListDTO;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.beans.PlayerPrizeStats;
import com.egr.snookerrank.model.TournamnetStats;
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

    @Query(value = "SELECT t.tournament_name as tournamentName," +
            "YEAR(e.event_date) as eventYear," +
            "e.has_seeds as hasSeeds," +
            "e.has_stats as hasStats," +
            "t.tournament_key as tournamnetKey," +
            "e.tv_channel as tvChannel," +
            "e.venue as venue " +
            "FROM event e JOIN tournament t ON e.tournament_key=t.tournament_key WHERE e.event_key= :EventKey",
            nativeQuery = true)
    List<Object[]> getEventTitle(@Param("EventKey") Integer eventKey);


    @Query(value = "SELECT m.match_key as matchKey, m.event_key as eventKey, Year(m.match_date) as matchDateYear, " +
            "m.round_no as roundNo, m.winner_key as winnerKey, m.loser_key as loserKey, " +
            "m.winner_score as winnerScore, m.loser_score as loserScore, m.is_bye as isBye, " +
            "m.group_letter as groupLetter, m.fdi_calc_done as fdiCalcDone," +
            "p.player_name playerWinnerName,p.player_key playerWinnerKey,p2.player_name playerLosserName,p2.player_key playerLosserKey " +
            "FROM [match] m JOIN player p ON m.winner_key=p.player_key " +
            "JOIN player p2 ON m.loser_key=p2.player_key " +
            "JOIN round r ON m.round_no=r.round_no " +
            "LEFT JOIN fixture f ON m.match_key=f.match_key " +
            "LEFT JOIN match_player_stats mps1 ON m.winner_key=mps1.player_key " +
            "AND m.match_key=mps1.match_key " +
            "LEFT JOIN match_player_stats mps2 ON m.loser_key=mps2.player_key AND " +
            "m.match_key=mps2.match_key WHERE m.event_key= :eventKey " +
            "ORDER BY r.order_num,m.round_no,group_letter,f.match_no,m.match_key"
            , nativeQuery = true)
    List<MatchResults> getMatchResult(@Param("eventKey") Integer eventKey);

    @Query(value = "SELECT r.round_name roundName,ep.round_no roundNo,ep.prize_money prizeMoney,r.num_matches numberOfMatches,t.country_name countryName,r.is_league isLeague,r.is_group isGroup FROM event_prize ep JOIN round r ON ep.round_no=r.round_no JOIN event e ON ep.event_key=e.event_key JOIN tournament t ON e.tournament_key=t.tournament_key " +
            "WHERE ep.event_key=:eventKey AND ep.round_no<>21 ORDER BY r.order_num,ep.round_no",
            nativeQuery = true)
    List<PrizeFund> getPrizeFunds(@Param("eventKey") Integer eventKey);

    @Query(value = "SELECT e.event_key as eventKey,t.tournament_name as tournamnetName,e.tournament_no as tournamentNo," +
            "FORMAT(e.event_date, 'dd-MM-yyyy') AS eventDate," +
            "FORMAT(e.start_date, 'dd-MM-yyyy') AS startDate," +
            "FORMAT(e.end_date, 'dd-MM-yyyy') AS endDate," +
            "e.currency_code as currencyCode,prize_fund as prizeFund,cc.conversion_rate as conversionRate,t.tournament_key as tournamnetKey,e.full_prizes as fullPrize,p.player_key as playerKey,p.player_name as winnerName,event_category as eventCategory" +
            " FROM event e JOIN tournament t ON e.tournament_key=t.tournament_key LEFT JOIN player_prize pp ON e.event_key=pp.event_key AND pp.round_no=21 AND (SELECT COUNT(*) FROM player_prize pp2 WHERE pp2.event_key=e.event_key AND pp2.round_no=21)=1 " +
            "LEFT JOIN player p ON pp.player_key=p.player_key LEFT JOIN currency cc ON e.currency_code=cc.currency_code WHERE (YEAR(event_date)=:year OR YEAR(start_date)= :year OR YEAR(end_date)= :year) " +
            "ORDER BY (CASE WHEN e.start_date>getdate() THEN e.start_date WHEN e.end_date<getdate() THEN e.end_date ELSE getdate() END),e.start_date,e.end_date,e.event_date,t.tournament_name,e.tournament_no"
            , nativeQuery = true)
    List<EventListDTO> getEventList(@Param("year") Integer year);

    @Query(value = "SELECT r.round_name,ep.prize_money " +
            "FROM event_prize ep JOIN round r ON ep.round_no=r.round_no JOIN event e ON ep.event_key=e.event_key " +
            "WHERE ep.event_key= :eventKey ORDER BY r.order_num DESC,ep.round_no DESC",
            nativeQuery = true)
    List<PrizeFundsDTO> getEventPrizeFund(@Param("eventKey") Integer eventKey);

    @Query(value = "SELECT e.event_key as eventKey, " +
            "Year(e.event_date) as year," +
            "p.player_name winnerName," +
            "p.player_key winnerKey," +
            "p2.player_name loserName," +
            "p2.player_key loserKey," +
            "m.winner_score as winnerScore," +
            "m.loser_score as loserScore," +
            "e.prize_fund as prizeFund " +
            "FROM [event] e LEFT JOIN player_prize pp ON e.event_key=pp.event_key AND pp.round_no=21 " +
            "LEFT JOIN player p ON pp.player_key=p.player_key " +
            "LEFT JOIN player_prize pp2 ON e.event_key=pp2.event_key AND pp2.round_no=20 " +
            "LEFT JOIN player p2 ON pp2.player_key=p2.player_key " +
            "LEFT JOIN match m ON e.event_key=m.event_key AND m.round_no=20 AND (m.winner_key=p.player_key OR m.winner_key=p2.player_key) " +
            "WHERE e.tournament_key=:tournamentKey " +
            "ORDER BY event_date DESC",
            nativeQuery = true)
    List<MatchResultDTO> getTournamentDetails(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT tournament_name FROM tournament WHERE tournament_key = :tournamnetKey", nativeQuery = true)
    String getTournamentName(@Param("tournamnetKey") Integer tournamnetKey);

//    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Wins' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE pp.round_no=21 AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC" +
//            " UNION ALL " +
//            "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC" +
//            " UNION ALL " +
//            "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Semi Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20 OR pp.round_no=19) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC" +
//            " UNION ALL " +
//            "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Quarter Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20 OR pp.round_no=19 OR pp.round_no=18) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC" +
//            " UNION ALL " +
//            "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Appearances' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN round r ON pp.round_no=r.round_no JOIN event e ON pp.event_key=e.event_key WHERE e.tournament_key=:tournamentKey AND r.allow_multi_prize=0 GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC" +
//            " UNION ALL " +
//            "SELECT TOP 1 p.player_key as playerKey, p.player_name as playerName, null,DATEDIFF(YEAR, p.dob, GETDATE()) as count, 'Youngest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date)" +
//            " UNIOJN ALL " +
//            "SELECT TOP 1 p.player_key as playerKey, p.player_name as playerName ,null,DATEDIFF(YEAR, p.dob, GETDATE()) as count, 'Oldest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date) DESC" +
//            " UNION ALL " +
//            "SELECT TOP 10 p.player_key as playerKey, p.player_name as playerName,Year(event_date),(SELECT ISNULL(SUM(century_breaks),0)as count, 'Most Century Breaks in a Tournament' as roundLabel FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key WHERE mps.player_key=p.player_key AND century_breaks>0 AND event_key=e.event_key) total_100 FROM player p,event e WHERE e.tournament_key=:tournamentKey ORDER BY total_100 DESC" +
//            " UNION ALL "+
//            "SELECT TOP 10 p.player_key as playerKey, p.player_name as playerName,null,ISNULL((SELECT MAX(century_breaks) FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key JOIN event e ON m.event_key=e.event_key WHERE mps.player_key=player.player_key AND century_breaks>0 AND tournament_key=:tournamentKey),0) count,'Most Century Breaks in a Match' FROM player ORDER BY most_100 DESC"
//            ,
//    nativeQuery = true)
//    List<TournamnetStats> getTournamnetstatsDetails(@Param("tournamentKey") Integer tournamentKey);


    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Wins' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE pp.round_no=21 AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostWins(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostFinals(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Semi Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20 OR pp.round_no=19) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostSemis(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Quarter Finals' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE (pp.round_no=21 OR pp.round_no=20 OR pp.round_no=19 OR pp.round_no=18) AND e.tournament_key=:tournamentKey GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostQuaters(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT p.player_key as playerKey,player_name as playerName,null,COUNT(*) as count,'Most Appearances' as roundLabel FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN round r ON pp.round_no=r.round_no JOIN event e ON pp.event_key=e.event_key WHERE e.tournament_key=:tournamentKey AND r.allow_multi_prize=0 GROUP BY p.player_key,p.player_name ORDER BY COUNT(*) DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostAppearances(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 1 p.player_key as playerKey, p.player_name as playerName, null,DATEDIFF(YEAR, p.dob, GETDATE()) as count, 'Youngest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date)",
            nativeQuery = true)
    List<TournamnetStats> getYoungestWinner(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 1 p.player_key as playerKey, p.player_name as playerName ,null,DATEDIFF(YEAR, p.dob, GETDATE()) as count, 'Oldest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date) DESC",
            nativeQuery = true)
    List<TournamnetStats> getOldestWinner(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 10 player_key as playerKey,player_name as playerName,Year(event_date) as year,(SELECT ISNULL(SUM(century_breaks),0)  FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key WHERE mps.player_key=p.player_key AND century_breaks>0 AND event_key=e.event_key) as count, 'Most Century Breaks in a Tournament' as roundLabel  FROM player p,event e WHERE e.tournament_key=:tournamentKey ORDER BY count DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostCenturyinTour(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 10 player_key as playerKey,player_name as playerName,null,ISNULL((SELECT MAX(century_breaks) FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key JOIN event e ON m.event_key=e.event_key WHERE mps.player_key=player.player_key AND century_breaks>0 AND tournament_key=:tournamentKey),0) count,'Most Century Breaks in a Match' as roundLabel FROM player ORDER BY count DESC"
            ,
            nativeQuery = true)
    List<TournamnetStats> getMostCenturyinMatch(@Param("tournamentKey") Integer tournamentKey);


}
