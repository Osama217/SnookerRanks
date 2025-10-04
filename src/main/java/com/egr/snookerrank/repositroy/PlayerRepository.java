package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.beans.*;
import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.EventListDTO;
import com.egr.snookerrank.dto.MatchResultsWithOrder;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.model.TournamnetStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {

    Player findByPlayerKey(Integer playerKey);
    List<Player> findAll();


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

    @Query(value = "SELECT TOP 1 p.player_key AS playerKey, p.player_name AS playerName, NULL,DATEDIFF(YEAR, dob, event_date) - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, dob, event_date), dob) > event_date THEN 1 ELSE 0 END AS count," +
            "'Youngest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date)",
            nativeQuery = true)
    List<TournamnetStats> getYoungestWinner(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 1 p.player_key AS playerKey, p.player_name AS playerName, NULL,DATEDIFF(YEAR, dob, event_date) - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, dob, event_date), dob) > event_date THEN 1 ELSE 0 END AS count," +
            "'Oldest Winner' as roundLabel FROM player_prize pp JOIN player p ON pp.player_key=p.player_key JOIN event e ON pp.event_key=e.event_key WHERE round_no=21 AND tournament_key=:tournamentKey AND NOT dob IS NULL ORDER BY DATEDIFF(day,dob,event_date) DESC",
            nativeQuery = true)
    List<TournamnetStats> getOldestWinner(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 10 player_key as playerKey,player_name as playerName,Year(event_date) as year,(SELECT ISNULL(SUM(century_breaks),0)  FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key WHERE mps.player_key=p.player_key AND century_breaks>0 AND event_key=e.event_key) as count, 'Most Century Breaks in a Tournament' as roundLabel  FROM player p,event e WHERE e.tournament_key=:tournamentKey ORDER BY count DESC",
            nativeQuery = true)
    List<TournamnetStats> getMostCenturyinTour(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 10 player_key as playerKey,player_name as playerName,null,ISNULL((SELECT MAX(century_breaks) FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key JOIN event e ON m.event_key=e.event_key WHERE mps.player_key=player.player_key AND century_breaks>0 AND tournament_key=:tournamentKey),0) count,'Most Century Breaks in a Match' as roundLabel FROM player ORDER BY count DESC"
            ,
            nativeQuery = true)
    List<TournamnetStats> getMostCenturyinMatch(@Param("tournamentKey") Integer tournamentKey);

    @Query(value = "SELECT TOP 1 player_key as playerKey,player_name as playerName,Year(event_date) as year,(SELECT ISNULL(SUM(max_breaks),0)  FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key WHERE mps.player_key=p.player_key AND max_breaks>0 AND event_key=e.event_key) as count, 'Most 147s' as roundLabel  FROM player p,event e WHERE e.tournament_key=:tournamentKey ORDER BY count DESC",
            nativeQuery = true)
    List<TournamnetStats> getMost147sInTournamnet(@Param("tournamentKey") Integer tournamentKey);
        // @Query(value = "SELECT TOP 1 player_key as playerKey, player_name as playerName, YEAR(event_date) as year, (SELECT COUNT(*) FROM match m JOIN match_player_stats mps ON m.match_key=mps.match_key WHERE mps.player_key=p.player_key AND mps.highest_break=147 AND m.event_key=e.event_key) as count, 'Most 147s' as roundLabel FROM player p, event e WHERE e.tournament_key=:tournamentKey ORDER BY count DESC", nativeQuery = true)
        // List<TournamnetStats> getMost147sInTournamnet(@Param("tournamentKey") Integer tournamentKey);

    String winner = "(SELECT COUNT(*) FROM match WHERE winner_key=:playerId AND loser_key=p.player_key AND is_bye=0 AND (winner_score > loser_score OR winner_score = 0))";
    String loser = "(SELECT COUNT(*) FROM match WHERE loser_key=:playerId AND winner_key=p.player_key AND is_bye=0 AND (winner_score > loser_score OR winner_score = 0))";
    String draw = "(SELECT COUNT(*) FROM match WHERE ((winner_key = :playerId AND loser_key = p.player_key) OR (loser_key = :playerId AND winner_key = p.player_key)) AND is_bye = 0 AND winner_score = loser_score AND winner_score > 0)";

    // Final query with proper casting and calculation of percentage
    String finalQuery =
            "SELECT player_key as playerKey, player_name as playerName, "
                    + winner + " AS wins, "
                    + draw + " AS draw, "
                    + loser + " AS losses, "
                    + "(CAST(" + winner + " AS float) + CAST(" + draw + " AS float) * 0.5) / "
                    + "(CAST(" + winner + " AS float) + CAST(" + draw + " AS float) + CAST(" + loser + " AS float)) *100 AS pcnt "
                    + "FROM player p "
                    + "WHERE (" + winner + " + " + loser + ") >= 2 "
                    + "ORDER BY pcnt ASC, losses DESC, wins ASC";

    @Query(value = finalQuery, nativeQuery = true)
    List<PlayerH2HStatsDTO> getCompleteH2HList(@Param("playerId") Integer playerId);

    @Query(value="SELECT * FROM player WHERE player_key IN (SELECT player_key FROM player_pro_card WHERE year=2024) AND NOT fdi IS NULL ORDER BY surname,player_name", nativeQuery = true)
    List<Player> findAllH2HPlayers();

    @Query(value = "SELECT t.tournament_key, e.event_key, t.tournament_name, e.event_date, e.event_category, r.round_name, m.winner_key, m.loser_key, w.player_name AS winner_name, l.player_name AS loser_name, m.winner_score, m.loser_score " +
            "FROM match m " +
            "JOIN event e ON m.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "JOIN round r ON m.round_no = r.round_no " +
            "JOIN player w ON m.winner_key = w.player_key " +
            "JOIN player l ON m.loser_key = l.player_key " +
            "WHERE m.is_bye = 0 " +
            "AND ((m.winner_key = :player1 AND m.loser_key = :player2) OR (m.winner_key = :player2 AND m.loser_key = :player1))",
            nativeQuery = true)
    List<Object[]> findMatchStats(@Param("player1") Integer player1, @Param("player2") Integer player2);

    @Query(value = "SELECT t.tournament_key as tournamnetKey, " +
            "e.event_key as eventKey," +
            " t.tournament_name as tournamnetName," +
            " CAST(e.event_date As DATE)AS eventDate," +
            " e.event_category eventCategory," +
            " r.round_name roundName," +
            " m.winner_key winnerKey, " +
            " m.loser_key loserKey" +
            ", w.player_name AS winnerName, l.player_name AS loserName, m.winner_score winnerScore, m.loser_score loserScore" +
            " FROM match m " +
            "JOIN event e ON m.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "JOIN round r ON m.round_no = r.round_no " +
            "JOIN player w ON m.winner_key = w.player_key " +
            "JOIN player l ON m.loser_key = l.player_key " +
            "WHERE m.is_bye = 0 " +
            "AND ((m.winner_key = :player1 AND m.loser_key = :player2) OR (m.winner_key = :player2 AND m.loser_key = :player1))" +
            " ORDER BY m.match_date DESC,e.event_date DESC,r.round_no DESC,m.match_key DESC",
            nativeQuery = true)
    List<MatchResultsWithOrder> findMatchStatsWithOrder(@Param("player1") Integer player1, @Param("player2") Integer player2);

    @Query(value = "SELECT \n" +
            "    SUM(CASE WHEN pots_made = -1 THEN 0 ELSE pots_made END) AS pots_made,\n" +
            "    SUM(CASE WHEN pots_attempted = -1 THEN 0 ELSE pots_attempted END) AS pots_attempted,\n" +
            "    SUM(CASE WHEN safety_made = -1 THEN 0 ELSE safety_made END) AS safety_made,\n" +
            "    SUM(CASE WHEN safety_attempted = -1 THEN 0 ELSE safety_attempted END) AS safety_attempted,\n" +
            "    SUM(CASE WHEN long_pots_made = -1 THEN 0 ELSE long_pots_made END) AS long_pots_made,\n" +
            "    SUM(CASE WHEN long_pots_attempted = -1 THEN 0 ELSE long_pots_attempted END) AS long_pots_attempted,\n" +
            "    SUM(CASE WHEN time_on_table = -1 THEN 0 ELSE time_on_table END) AS time_on_table,\n" +
            "    SUM(CASE WHEN shots_taken = -1 THEN 0 ELSE shots_taken END) AS shots_taken,\n" +
            "    SUM(CASE WHEN century_breaks = -1 THEN 0 ELSE century_breaks END) AS century_breaks,\n" +
            "    SUM(CASE WHEN fifty_breaks = -1 THEN 0 ELSE fifty_breaks END) AS fifty_breaks,\n" +
            "    SUM(CASE WHEN seventy_breaks = -1 THEN 0 ELSE seventy_breaks END) AS seventy_breaks,\n" +
            "    SUM(CASE WHEN frames_won = -1 THEN 0 ELSE frames_won END) AS frames_won,\n" +
            "    SUM(CASE WHEN frames_played = -1 THEN 0 ELSE frames_played END) AS frames_played,\n" +
            "    SUM(CASE WHEN max_breaks = -1 THEN 0 ELSE max_breaks END) AS max_breaks,\n" +
            "    MAX(CASE WHEN highest_break = -1 THEN 0 ELSE highest_break END) AS highest_break,\n" +
            "    SUM(CASE WHEN points_scored = -1 THEN 0 ELSE points_scored END) AS points_scored,\n" +


        //% matches won
        "    (\n" +
        "        SELECT CAST(\n" +
        "            COUNT(DISTINCT CASE WHEN m2.winner_key = :playerKey THEN m2.match_key END) * 100.0 /\n" +
        "            NULLIF(COUNT(DISTINCT m2.match_key), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +
        "        FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS match_win_percentage,\n" +

        //% deciding
        "    (\n" +
        "        SELECT CAST(\n" +
        "            COUNT(DISTINCT CASE WHEN mps2.player_key = m2.winner_key THEN m2.match_key END) * 100.0 /\n" +
        "            NULLIF(COUNT(DISTINCT m2.match_key), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +
        "        FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND mps2.frame_scores IS NOT NULL\n" +
        "          AND (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          ) = (m2.winner_score + m2.loser_score)\n" +
        "          AND (m2.winner_score + m2.loser_score) = (2 * m2.winner_score - 1)\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS deciders_win_percentage\n," +

        // Avg points deficit in frames won
        "    (\n" +
        "        SELECT AVG(CAST(player_points - opponent_points AS FLOAT))\n" +
        "        FROM (\n" +
        "            SELECT \n" +
        "                CASE WHEN CHARINDEX('-', clean_value) > 0 \n" +
        "                    THEN TRY_CAST(LEFT(clean_value, CHARINDEX('-', clean_value) - 1) AS INT) \n" +
        "                    ELSE NULL END AS player_points,\n" +
        "                CASE WHEN CHARINDEX('-', clean_value) > 0 \n" +
        "                    THEN TRY_CAST(SUBSTRING(clean_value, CHARINDEX('-', clean_value) + 1, 5) AS INT) \n" +
        "                    ELSE NULL END AS opponent_points\n" +
        "            FROM match_player_stats mps2\n" +
        "            JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "            CROSS APPLY (\n" +
        "                SELECT LEFT(value, CHARINDEX('(', value + '(') - 1) AS clean_value\n" +
        "                FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "                WHERE CHARINDEX('-', value) > 0\n" +
        "            ) AS frame_data\n" +
        "            WHERE mps2.player_key = :playerKey\n" +
        "              AND m2.winner_key IS NOT NULL\n" +
        "              AND m2.loser_key IS NOT NULL\n" +
        "              AND mps2.frame_scores IS NOT NULL\n" +
        "              AND m2.match_date >= :fromDate\n" +
        "              AND m2.match_date <= :toDate\n" +
        "        ) AS frame_stats\n" +
        "        WHERE player_points > opponent_points\n" +
        "    ) AS avg_points_deficit_won_frames\n" +
        "\n, " +

        // Avg points deficit in frames lost
        "    (\n" +
        "        SELECT AVG(CAST(player_points - opponent_points AS FLOAT))\n" +
        "        FROM (\n" +
        "            SELECT \n" +
        "                CASE WHEN CHARINDEX('-', clean_value) > 0 \n" +
        "                    THEN TRY_CAST(LEFT(clean_value, CHARINDEX('-', clean_value) - 1) AS INT) \n" +
        "                    ELSE NULL END AS player_points,\n" +
        "                CASE WHEN CHARINDEX('-', clean_value) > 0 \n" +
        "                    THEN TRY_CAST(SUBSTRING(clean_value, CHARINDEX('-', clean_value) + 1, 5) AS INT) \n" +
        "                    ELSE NULL END AS opponent_points\n" +
        "            FROM match_player_stats mps2\n" +
        "            JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "            CROSS APPLY (\n" +
        "                SELECT LEFT(value, CHARINDEX('(', value + '(') - 1) AS clean_value\n" +
        "                FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "                WHERE CHARINDEX('-', value) > 0\n" +
        "            ) AS frame_data\n" +
        "            WHERE mps2.player_key = :playerKey\n" +
        "              AND m2.winner_key IS NOT NULL\n" +
        "              AND m2.loser_key IS NOT NULL\n" +
        "              AND mps2.frame_scores IS NOT NULL\n" +
        "              AND m2.match_date >= :fromDate\n" +
        "              AND m2.match_date <= :toDate\n" +
        "        ) AS frame_stats\n" +
        "        WHERE player_points < opponent_points\n" +
        "    ) AS avg_points_deficit_lost_frames\n" +
        "\n, " +

        //opening frames won
        "\n" +
        "    (\n" +
        "        SELECT CAST(\n" +
        "            SUM(CASE WHEN TRY_CAST(LEFT(opening_frame, CHARINDEX('-', opening_frame) - 1) AS INT) > \n" +
        "                       TRY_CAST(SUBSTRING(opening_frame, CHARINDEX('-', opening_frame) + 1, 5) AS INT) \n" +
        "                THEN 1 ELSE 0 END) * 100.0 /\n" +
        "            NULLIF(COUNT(DISTINCT m2.match_key), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +
        "        FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        CROSS APPLY (\n" +
        "            SELECT TOP 1 value AS opening_frame \n" +
        "            FROM STRING_SPLIT(mps2.frame_scores, ';') \n" +
        "            WHERE CHARINDEX('-', value) > 0\n" +
        "        ) AS frame_data\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND mps2.frame_scores IS NOT NULL\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS opening_frame_win_percentage\n" +
        "\n, " +

        // opening 2
        "    (\n" +
        "        SELECT CAST(SUM(CASE \n" +
        "            WHEN TRY_CAST(LEFT(fd1.opening_frame1, CHARINDEX('-', fd1.opening_frame1) - 1) AS INT) > \n" +
        "                 TRY_CAST(SUBSTRING(fd1.opening_frame1, CHARINDEX('-', fd1.opening_frame1) + 1, 5) AS INT) \n" +
        "             AND TRY_CAST(LEFT(fd2.opening_frame2, CHARINDEX('-', fd2.opening_frame2) - 1) AS INT) > \n" +
        "                 TRY_CAST(SUBSTRING(fd2.opening_frame2, CHARINDEX('-', fd2.opening_frame2) + 1, 5) AS INT) \n" +
        "            THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(DISTINCT m2.match_key), 0) AS DECIMAL(5,2))\n" +
        "        FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        JOIN player p2 ON mps2.player_key = p2.player_key\n" +
        "        CROSS APPLY ( \n" +
        "            SELECT TOP 1 value AS opening_frame1 \n" +
        "            FROM STRING_SPLIT(mps2.frame_scores, ';') \n" +
        "            WHERE CHARINDEX('-', value) > 0 \n" +
        "        ) AS fd1 \n" +
        "        CROSS APPLY ( \n" +
        "            SELECT TOP 1 value AS opening_frame2 \n" +
        "            FROM ( \n" +
        "                SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn \n" +
        "                FROM STRING_SPLIT(mps2.frame_scores, ';') \n" +
        "                WHERE CHARINDEX('-', value) > 0 \n" +
        "            ) x WHERE rn = 2 \n" +
        "        ) AS fd2 \n" +
        "        WHERE mps2.player_key = :playerKey \n" +
        "          AND m2.winner_key IS NOT NULL \n" +
        "          AND m2.loser_key IS NOT NULL \n" +
        "          AND mps2.frame_scores IS NOT NULL \n" +
        "          AND fd1.opening_frame1 IS NOT NULL \n" +
        "          AND fd2.opening_frame2 IS NOT NULL \n" +
        "          AND m2.match_date >= :fromDate \n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS opening_2_frames_win_percentage\n" +
        "\n," +


        //50+ in deciders
        "    (\n" +
        "        SELECT CAST(\n" +
        "            SUM(CASE WHEN \n" +
        "                CHARINDEX('(', f.value) > 0 AND \n" +
        "                TRY_CAST(SUBSTRING(f.value, CHARINDEX('(', f.value) + 1, \n" +
        "                    CHARINDEX(')', f.value) - CHARINDEX('(', f.value) - 1) AS INT) >= 50 \n" +
        "            THEN 1 ELSE 0 END) * 100.0 / \n" +
        "            NULLIF(COUNT(*), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +

        "    FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        CROSS APPLY (\n" +
        "            SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn \n" +
        "            FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "        ) AS f\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND mps2.frame_scores IS NOT NULL\n" +
        "          AND (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          ) = (m2.winner_score + m2.loser_score)\n" +
        "          AND (m2.winner_score + m2.loser_score) = (2 * m2.winner_score - 1)\n" +
        "          AND f.rn = (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          )\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS breaks_50_plus_deciders_percentage\n" +
        "\n," +

        //70+ in deciders
        "    (\n" +
        "        SELECT CAST(\n" +
        "            SUM(CASE WHEN \n" +
        "                CHARINDEX('(', f.value) > 0 AND \n" +
        "                TRY_CAST(SUBSTRING(f.value, CHARINDEX('(', f.value) + 1, \n" +
        "                    CHARINDEX(')', f.value) - CHARINDEX('(', f.value) - 1) AS INT) >= 70 \n" +
        "            THEN 1 ELSE 0 END) * 100.0 / \n" +
        "            NULLIF(COUNT(*), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +

        "    FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        CROSS APPLY (\n" +
        "            SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn \n" +
        "            FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "        ) AS f\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND mps2.frame_scores IS NOT NULL\n" +
        "          AND (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          ) = (m2.winner_score + m2.loser_score)\n" +
        "          AND (m2.winner_score + m2.loser_score) = (2 * m2.winner_score - 1)\n" +
        "          AND f.rn = (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          )\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS breaks_70_plus_deciders_percentage\n" +
        "\n, " +

        //100+ in deciders
        "    (\n" +
        "        SELECT CAST(\n" +
        "            SUM(CASE WHEN \n" +
        "                CHARINDEX('(', f.value) > 0 AND \n" +
        "                TRY_CAST(SUBSTRING(f.value, CHARINDEX('(', f.value) + 1, \n" +
        "                    CHARINDEX(')', f.value) - CHARINDEX('(', f.value) - 1) AS INT) >= 100 \n" +
        "            THEN 1 ELSE 0 END) * 100.0 / \n" +
        "            NULLIF(COUNT(*), 0) AS DECIMAL(5,2)\n" +
        "        )\n" +

        "    FROM match_player_stats mps2\n" +
        "        JOIN match m2 ON m2.match_key = mps2.match_key\n" +
        "        CROSS APPLY (\n" +
        "            SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn \n" +
        "            FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "        ) AS f\n" +
        "        WHERE mps2.player_key = :playerKey\n" +
        "          AND m2.winner_key IS NOT NULL\n" +
        "          AND m2.loser_key IS NOT NULL\n" +
        "          AND mps2.frame_scores IS NOT NULL\n" +
        "          AND (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          ) = (m2.winner_score + m2.loser_score)\n" +
        "          AND (m2.winner_score + m2.loser_score) = (2 * m2.winner_score - 1)\n" +
        "          AND f.rn = (\n" +
        "              SELECT COUNT(*) FROM STRING_SPLIT(mps2.frame_scores, ';')\n" +
        "          )\n" +
        "          AND m2.match_date >= :fromDate\n" +
        "          AND m2.match_date <= :toDate\n" +
        "    ) AS breaks_100_plus_deciders_percentage\n" +
        "\n " +

            "FROM \n" +
            "    match_player_stats mps\n" +
            "JOIN \n" +
            "    match m ON mps.match_key = m.match_key\n" +
            "JOIN \n" +
            "    event e ON m.event_key = e.event_key\n" +
          
            "WHERE \n" +
            "    m.match_date >= :fromDate \n" +
            "    AND m.match_date <= :toDate  \n" +
            "  AND m.winner_key   IS NOT NULL\n" +
            "  AND m.loser_key    IS NOT NULL \n" +
            "    AND mps.player_key = :playerKey;",nativeQuery = true)
    
    List<Map<String,Integer>> getTalentDEtailsByDate(@Param("playerKey") Integer playerKey, @Param("fromDate") String fromDate,
                                                    @Param("toDate") String toDate);
    
    @Query(value = "SELECT rank_name as rankName,rank_text_key as rankKey,stat_type as statType,field1,field2 FROM rank_text " +
            "WHERE is_talent_portal=1  ORDER BY talent_portal_order",nativeQuery = true)
    List<RankFields> getTalentPortalRanks();

    @Query(value = "SELECT player.player_key as playerKey," +
            "player_name as playerName," +
            "age," +
            "fdi_rank as fdiRank," +
            "world_rank as worldRank," +
            "country_name as countryName," +
            "(world_rank-fdi_rank) diff " +
            "FROM player inner join player_pro_card on player.player_key=player_pro_card.player_key  " +
            "WHERE NOT world_rank IS NULL AND NOT fdi_rank IS NULL ORDER BY diff DESC\n",nativeQuery = true)
    List<FDIComparisonDTO> gettalenetPortaFDIComparison();

    @Query(value = "SELECT rank_text_key,rank_name FROM rank_text WHERE is_ranking=1 ORDER BY order_num\n",nativeQuery = true)
    List<Object[]> getRanksForRanking();

    @Query(value = "SELECT DISTINCT country_name FROM player WHERE country_name<>'' AND NOT country_name IS NULL ORDER BY country_name",nativeQuery = true)
    List<String> getCountryNames();

    @Query(value = "SELECT player_key as playerKey," +
            " player_name as PlayerName, " +
            "country_name as countryName, " +
            "fdi " +
            "FROM player " +
            "WHERE fdi IS NOT NULL " +
            "AND (:country IS NULL OR country_name = :country) " +
            "AND (:isWoman IS NULL OR :isWoman = 1 AND is_woman = 1) " +
            "AND (age < :maxAge AND age <> 0) " +
            "AND fdi_matches > 20 " +
            "ORDER BY fdi DESC",
            nativeQuery = true)
    List<PlayerDTO> findPlayers(@Param("rankKey") int rankKey,
                             @Param("country") String countryName,
                             @Param("isWoman") Integer isWoman,  // Changed to `Boolean`
                             @Param("maxAge") Integer maxAge);



    @Query(value = "SELECT p.player_key AS playerKey, p.player_name AS playerName, p.country_name AS countryName, " +
            "LTRIM(STR(ROUND(SUM(prize_money * conversion_rate), 0) ,20,0 ))AS stats " +
            "FROM player p " +
            "JOIN player_prize pp ON p.player_key = pp.player_key " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "WHERE 1=1 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "GROUP BY p.player_key, p.player_name, p.country_name " +
            "HAVING SUM(prize_money) > 0 " +
            "ORDER BY SUM(prize_money) DESC",
            nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerPrizeSummary(@Param("tournamentKey") Integer tournamentKey,
                                                         @Param("dateFrom") String dateFrom,
                                                         @Param("dateTo") String dateTo);

    @Query(value = "SELECT p.player_key playerKey, p.player_name playerName, p.country_name countryName,CAsT( COUNT(*) as varchar)stats " +
            "FROM player p JOIN player_prize pp ON p.player_key=pp.player_key " +
            "JOIN round r ON pp.round_no=r.round_no " +
            "JOIN event e ON pp.event_key=e.event_key " +
            "WHERE 1=1  " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.allow_multi_prize=0 " +
            "GROUP BY p.player_key, p.player_name, p.country_name " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerApplications(@Param("tournamentKey") Integer tournamentKey,
                                                         @Param("dateFrom") String dateFrom,
                                                         @Param("dateTo") String dateTo);

    @Query(value = "SELECT p.player_key playerKey, p.player_name playerName, p.country_name countryName, CAST(COUNT(*) as varchar)stats " +
            "FROM player p JOIN match m ON p.player_key=m.winner_key OR p.player_key=m.loser_key " +
            "JOIN round r ON m.round_no=r.round_no " +
            "JOIN event e ON m.event_key=e.event_key " +
            "WHERE 1=1 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND m.is_bye=0 " +
            "GROUP BY p.player_key, p.player_name, p.country_name " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerMatches(@Param("tournamentKey") Integer tournamentKey,
                                                    @Param("dateFrom") String dateFrom,
                                                    @Param("dateTo") String dateTo);

    @Query(value = "SELECT p.player_key playerKey, p.player_name playerName, p.country_name countryName, CAST(COUNT(*) as varchar) stats " +
            "FROM player p JOIN player_prize pp ON p.player_key=pp.player_key " +
            "JOIN round r ON pp.round_no=r.round_no " +
            "JOIN event e ON pp.event_key=e.event_key " +
            "WHERE 1=1 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.allow_multi_prize=0 " +
            "AND pp.prize_money > 0 " +
            "GROUP BY p.player_key, p.player_name, p.country_name " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerCashes(@Param("tournamentKey") Integer tournamentKey,
                                                   @Param("dateFrom") String dateFrom,
                                                   @Param("dateTo") String dateTo);

    @Query(value = "SELECT p.player_key AS playerKey, p.player_name AS playerName, p.country_name AS countryName, " +
            "Cast( ((CONVERT(float, points_scored) * 3) / CONVERT(float, Snooker_thrown)) as varchar)AS stats, " +
            "FROM player p " +
            "JOIN match m ON p.player_key = m.winner_key " +
            "JOIN event e ON m.event_key = e.event_key " +
            "JOIN round r ON m.round_no = r.round_no " +
            "JOIN match_player_stats mps ON m.winner_key = mps.player_key AND m.match_key = mps.match_key " +
            "JOIN player p2 ON m.loser_key = p2.player_key " +
            "WHERE mps.Snooker_thrown > 0 AND (m.winner_score = 0 OR m.winner_score > m.loser_score) " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "ORDER BY winnerAverage DESC", nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getWinnerAverage(@Param("tournamentKey") Integer tournamentKey,
                                                    @Param("dateFrom") String dateFrom,
                                                    @Param("dateTo") String dateTo);

    @Query(value = "SELECT p.player_key AS playerKey, p.player_name AS playerName, p.country_name AS countryName, " +
            "Cast(COUNT(*) as varchar)AS numWins " +
            "FROM player p " +
            "JOIN player_prize pp ON p.player_key = pp.player_key " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN tournament t ON e.tournament_key = t.tournament_key " +
            "WHERE pp.round_no = 21 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "GROUP BY p.player_key, p.player_name, p.country_name " +
            "HAVING COUNT(*) > 0 " +
            "ORDER BY numWins DESC", nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerWins(@Param("tournamentKey") Integer tournamentKey,
                                                 @Param("dateFrom") String dateFrom,
                                                 @Param("dateTo") String dateTo);









    @Query(value = "SELECT p.country_name AS countryName, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 2300) AS wins, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +
            "AND r.order_num >= 2200 AND r.order_num < 2300) AS ru, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
             "AND r.order_num >= 1900 AND r.order_num <= 2000) AS sf, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
             "AND r.order_num >= 1760 AND r.order_num <= 1899) AS qf, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
             "AND r.order_num >= 1680 AND r.order_num <= 1750) AS l16, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1510 AND r.order_num <= 1670) AS l32, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.country_name = p.country_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1370 AND r.order_num <= 1500) AS l64, " +
            "COUNT(*) AS placings " +
            "FROM player p " +
            "JOIN player_prize pp ON p.player_key = pp.player_key " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "WHERE 1 = 1 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part            "AND p.country_name IS NOT NULL " +
            "AND r.allow_multi_prize = 0 " +
            "GROUP BY p.country_name " +
            "ORDER BY wins DESC, ru DESC, sf DESC, qf DESC, l16 DESC, l32 DESC, l64 DESC, placings DESC",
            nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerCountryStats(@Param("tournamentKey") Integer tournamentKey,
                                                         @Param("dateFrom") String dateFrom,
                                                         @Param("dateTo") String dateTo);


    @Query(value = "SELECT p.player_key as playerKey, p.player_name as playerName, p.country_name AS countryName, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 2300) AS wins, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +
            "AND r.order_num >= 2200 AND r.order_num < 2300) AS ru, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1900 AND r.order_num <= 2000) AS sf, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1760 AND r.order_num <= 1899) AS qf, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1680 AND r.order_num <= 1750) AS l16, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1510 AND r.order_num <= 1670) AS l32, " +
            "(SELECT COUNT(*) FROM player_prize pp " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "JOIN player p2 ON pp.player_key = p2.player_key " +
            "WHERE p2.player_name = p.player_name " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part
            "AND r.order_num >= 1370 AND r.order_num <= 1500) AS l64, " +
            "COUNT(*) AS placings " +
            "FROM player p " +
            "JOIN player_prize pp ON p.player_key = pp.player_key " +
            "JOIN event e ON pp.event_key = e.event_key " +
            "JOIN round r ON pp.round_no = r.round_no " +
            "WHERE 1 = 1 " +
            "AND e.tournament_key = :tournamentKey " + // Hardcoded part
            "AND e.event_date >= :dateFrom " +         // Hardcoded part
            "AND e.event_date <= :dateTo " +           // Hardcoded part            "AND p.country_name IS NOT NULL " +
            "AND r.allow_multi_prize = 0 " +
            "GROUP BY p.player_key,player_name,p.country_name " +
            "ORDER BY wins DESC, ru DESC, sf DESC, qf DESC, l16 DESC, l32 DESC, l64 DESC, placings DESC",
            nativeQuery = true)
    List<PlayerTournamnetStatsDTO> getPlayerSuccessStats(@Param("tournamentKey") Integer tournamentKey,
                                                         @Param("dateFrom") String dateFrom,
                                                         @Param("dateTo") String dateTo);


    @Query(value = "SELECT * FROM match_player_stats WHERE match_key = :matchKey AND player_key = :playerKey", nativeQuery = true)
    List<Map<String, Object>> getMatchPlayerStats(@Param("matchKey") Integer matchKey, @Param("playerKey") Integer playerKey);

    @Query(value = "SELECT top 12 e.event_key as eventKey,t.tournament_name as tournamnetName," +
            "FORMAT(e.event_date,'dd-MM-yyyy')as date,"+
            "YEAR(e.event_date) as year,"+
            "p.player_name winnerName," +
            "p.player_key winnerKey," +
            "p2.player_name loserName," +
            "p2.player_key loserKey," +
            "m.winner_score as winnerScore," +
            "m.loser_score as loserScore," +
            "e.prize_fund as prizeFund " +
            "FROM [event] e LEFT JOIN player_prize pp ON e.event_key=pp.event_key AND pp.round_no=21 " +
            "join tournament t on e.tournament_key=t.tournament_key " +
            "LEFT JOIN player p ON pp.player_key=p.player_key " +
            "LEFT JOIN player_prize pp2 ON e.event_key=pp2.event_key AND pp2.round_no=20" +
            "LEFT JOIN player p2 ON pp2.player_key=p2.player_key " +
            "LEFT JOIN match m ON e.event_key=m.event_key AND m.round_no=20 AND (m.winner_key=p.player_key OR m.winner_key=p2.player_key) " +
            "where winner_key is not null or loser_key is not null " +
            "ORDER BY event_date DESC",nativeQuery = true)
    List<MatchResultDTO> getLatestResult();
}
