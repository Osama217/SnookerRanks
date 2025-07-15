package com.egr.snookerrank.repositroy.playerstats;

import com.egr.snookerrank.dto.TournamentDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public class PlayerStatsRepositoryImpl implements PlayerStatsRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public record MaxBreakStatsDTO(Integer totalMaxBreaks, Integer totalCenturyBreaks) {}

    @Override
    public List<Object[]> findPlayersWithFilters(LocalDate dDateFrom, LocalDate dDateTo, Integer eventKey, TournamentDTO tournament, Integer year, Integer rankKey) {
        StringBuilder sql = new StringBuilder();
            if (rankKey.equals(50)) {
                sql.append(BASE_QUERY_CASE_50);
            } else {
                sql.append(BASE_QUERY);
            }
            setAdditionalFieldsInQuery(sql,tournament,year,eventKey);
            if (rankKey.equals(51)) {
                sql.append(RANK_KEY_51);
            } else if (rankKey.equals(52)) {
                sql.append(RANK_KEY_52);
            } else if (rankKey.equals(53)) {
                sql.append(RANK_KEY_53);
            } else if (rankKey.equals(54)) {
                sql.append(RANK_KEY_54);
            }
            if (rankKey.equals(50)) {
                sql.append(GROUP_BY_CASE_50);
            } else {
                sql.append(GROUP_BY);
            }
            Query query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("dDateFrom", dDateFrom);
            query.setParameter("dDateTo", dDateTo);
            setAdditionalParameters(query,year,eventKey);
            return query.getResultList();
    }


    private static void setAdditionalFieldsInQuery(StringBuilder sql,TournamentDTO tournament, Integer year, Integer eventKey) {
        if (TournamentDTO.All_Ranking.equals(tournament)) {
            sql.append(TOURNAMENT_ALL_RANK);

        } else if (TournamentDTO.Triple_Crown.equals(tournament)) {
            sql.append(TOURNAMENT_3C);

        } else if (TournamentDTO.World_Championships.equals(tournament)) {
            sql.append(TOURNAMENT_WC);
        }
        if (null != year && year > 0) {
            sql.append(TOUR_YEAR);
        }
        if (eventKey != null && eventKey > 0) {
            sql.append(TOUR_EVENT);
        }
    }
    private static void setAdditionalParameters(Query query, Integer year, Integer eventKey) {
        if (null != year && year > 0) {
            query.setParameter("year", year);
        }
        if (eventKey != null && eventKey > 0) {
            query.setParameter("event", eventKey);
        }
    }
    @Override
    public List<Object[]> findPlayersWithStatsRankingFilters(String statType, String field1, String field2,TournamentDTO tournament,Integer year, Integer eventKey,
                                                             LocalDate dateFrom, LocalDate dateTo, Integer minMatches, Boolean orderAsc, Integer topLimit) {


        // new stat types
        if ("%deciding_frames_won".equalsIgnoreCase(statType)) {
            return getDecidingFrameWinStats(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }             

        if ("av_deficit_frames_won".equalsIgnoreCase(statType)) {
            return getAverageDeficitFramesWon(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }

        if ("av_deficit_frames_lost".equalsIgnoreCase(statType)) {
            return getAverageDeficitFramesLost(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }      

        if ("50_in_deciding".equalsIgnoreCase(statType)) {
            return getFiftyPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        } 

        if ("70_in_deciding".equalsIgnoreCase(statType)) {
            return getSeventyPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        } 

        if ("century_in_deciding".equalsIgnoreCase(statType)) {
            return getHundredPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }       

        if ("%matches_won".equalsIgnoreCase(statType)){
            return getMatchWinPercentagePerPlayer(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }  

        if ("%opening_frames_won".equalsIgnoreCase(statType)){
            System.out.println(statType);
            System.out.println(year);
            return getOpeningFrameWinPercentage(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        } 

        if ("%opening_two_frames_won".equalsIgnoreCase(statType)){
            System.out.println(statType);
            System.out.println(year);

            return getOpeningTwoFrameWinPercentage(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }            

        // Base Query
        StringBuilder sql = new StringBuilder("SELECT TOP " + topLimit + " p.player_key, p.player_name, p.country_name, ");

        // Determine main statistical field logic
        if(null == minMatches || minMatches <= 0){
            minMatches = 10;
        }
        if("D".equals(statType) || "A".equals(statType)|| "P".equals(statType))
            sql.append("SUM(").append(field1).append("), SUM(").append(field2).append("), ");
        String mainStatField = determineMainStatField(statType, field1, field2);
        sql.append(mainStatField);

        // Join Conditions
        sql.append(" FROM player p ")
                .append("JOIN match_player_stats mps ON p.player_key = mps.player_key ")
                .append("JOIN match m ON mps.match_key = m.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                .append("WHERE ").append(field1).append(" <> -1 ");

        // Additional Filters
        if (requiresField2(statType)) {
            sql.append(" AND ").append(field2).append(" <> -1 ");
        }

        sql.append(" AND m.match_date >= :dateFrom ")
                .append(" AND m.match_date <= :dateTo ");
        setAdditionalFieldsInQuery(sql,tournament,year,eventKey);

        sql.append(" GROUP BY p.player_key, p.player_name, p.country_name ");

        // Having Clause (for specific stat types)
        if (requiresField2(statType)) {
            sql.append(" HAVING SUM(").append(field2).append(") >= :minMatches ");
        }

        // Sorting
        sql.append(" ORDER BY ").append(mainStatField).append(orderAsc ? " ASC" : " DESC");
        // Create Query
        Query query = entityManager.createNativeQuery(sql.toString());

        // Set Parameters
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        if (requiresField2(statType)) {
            query.setParameter("minMatches", minMatches);
        }
        setAdditionalParameters(query,year,eventKey);


        return query.getResultList();
    }

    /**
     * Determines the main statistical field calculation based on statType.
     */
    private String determineMainStatField(String statType, String field1, String field2) {
        return switch (statType) {
            case "D", "A", "P" -> "convert(float,SUM(" + field1 + "))" + ("A".equals(statType) ? " * 3" : "") +
                   " / convert(float,SUM(" + field2 + ") )";
            case "X" -> "MAX(" + field1 + ")";
            default -> "SUM(" + field1 + ")";
        };
    }

    /**
     * Checks if field2 is required for calculations.
     */
    private boolean requiresField2(String statType) {
        return "D".equals(statType) || "A".equals(statType) || "P".equals(statType);
    }

    public MaxBreakStatsDTO getTotalMaxBreaks(Integer playerKey) {
        String sql = "SELECT COALESCE(SUM(max_breaks), 0),COALESCE(SUM(century_breaks), 0) FROM match_player_stats WHERE player_key = :playerKey";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("playerKey", playerKey);
        Object[] result = (Object[]) query.getSingleResult();
        return  new MaxBreakStatsDTO(((Number) result[0]).intValue(), ((Number) result[1]).intValue());
    }

    private List<Object[]> getMatchWinPercentagePerPlayer(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("p.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("COUNT(DISTINCT CASE WHEN m.winner_key = mps.player_key THEN m.match_key END) AS matches_won, ") // ✅ fixed
            .append("COUNT(DISTINCT m.match_key) AS total_matches, ") // ✅ fixed
            .append("CAST(COUNT(DISTINCT CASE WHEN m.winner_key = mps.player_key THEN m.match_key END) * 100.0 / COUNT(DISTINCT m.match_key) AS DECIMAL(5,2)) AS win_percentage ") // ✅ fixed
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("WHERE m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ")
            .append("AND m.winner_key IS NOT NULL ");

        // Optional filters
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY p.player_key, p.player_name, p.country_name ")
            .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getDecidingFrameWinStats(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")

            // Wins: distinct decider matches won by player
            .append("COUNT(DISTINCT CASE WHEN mps.player_key = m.winner_key THEN m.match_key END) AS deciders_won, ")

            // Played: distinct decider matches played by player
            .append("COUNT(DISTINCT m.match_key) AS deciders_played, ")

            // Percentage
            .append("CAST(COUNT(DISTINCT CASE WHEN mps.player_key = m.winner_key THEN m.match_key END) * 100.0 / NULLIF(COUNT(DISTINCT m.match_key),0) AS DECIMAL(5,2)) AS win_percentage ")

            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON m.match_key = mps.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")

            .append("WHERE mps.player_key IN ( ")
            .append("    SELECT mps2.player_key ")
            .append("    FROM match_player_stats mps2 ")
            .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
            .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
            .append("      AND m2.match_date >= :dateFrom ")
            .append("      AND m2.match_date <= :dateTo ")
            .append("    GROUP BY mps2.player_key ")
            .append("    HAVING COUNT(DISTINCT m2.match_key) >= 20 ")
            .append(") ")

            .append("AND mps.frame_scores IS NOT NULL ")
            .append("AND ( ")
            .append("    SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") = (m.winner_score + m.loser_score) ") 
            .append("AND (m.winner_score + m.loser_score) = (2 * m.winner_score - 1) ")
            .append("AND m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ");


        // Optional filters for tournament, year, event
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name ")
            .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getAverageDeficitFramesWon(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("player_key, player_name, country_name, ")
            .append("0 AS dummy1, 0 AS dummy2, ")
            .append("AVG(CAST(player_points - opponent_points AS FLOAT)) AS avg_deficit ")
            .append("FROM ( ")
            .append("SELECT ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("CASE WHEN CHARINDEX('-', clean_value) > 0 ")
            .append("     THEN TRY_CAST(LEFT(clean_value, CHARINDEX('-', clean_value) - 1) AS INT) ")
            .append("     ELSE NULL END AS player_points, ")
            .append("CASE WHEN CHARINDEX('-', clean_value) > 0 ")
            .append("     THEN TRY_CAST(SUBSTRING(clean_value, CHARINDEX('-', clean_value) + 1, 5) AS INT) ")
            .append("     ELSE NULL END AS opponent_points ")
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT LEFT(value, CHARINDEX('(', value + '(') - 1) AS clean_value ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append("    WHERE CHARINDEX('-', value) > 0 ")
            .append(") AS frame_data ")
            .append("WHERE m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ")
            .append("AND mps.frame_scores IS NOT NULL ")
            
            // ✅ Only include players with >= 20 matches in the same date range
            .append("AND mps.player_key IN ( ")
            .append("    SELECT mps2.player_key ")
            .append("    FROM match_player_stats mps2 ")
            .append("    JOIN match m2 ON mps2.match_key = m2.match_key ")
            .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
            .append("      AND m2.match_date >= :dateFrom ")
            .append("      AND m2.match_date <= :dateTo ")
            .append("    GROUP BY mps2.player_key ")
            .append("    HAVING COUNT(DISTINCT m2.match_key) >= 20 ")
            .append(") ")

            // End subquery
            .append(") AS frame_stats ")
            .append("WHERE player_points > opponent_points ")
            .append("GROUP BY player_key, player_name, country_name ")
            .append("ORDER BY avg_deficit ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getAverageDeficitFramesLost(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("player_key, player_name, country_name, ")
            .append("0 AS dummy1, 0 AS dummy2, ")
            .append("AVG(CAST(player_points - opponent_points AS FLOAT)) AS avg_deficit ")
            .append("FROM ( ")
            .append("SELECT ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("CASE WHEN CHARINDEX('-', clean_value) > 0 ")
            .append("     THEN TRY_CAST(LEFT(clean_value, CHARINDEX('-', clean_value) - 1) AS INT) ")
            .append("     ELSE NULL END AS player_points, ")
            .append("CASE WHEN CHARINDEX('-', clean_value) > 0 ")
            .append("     THEN TRY_CAST(SUBSTRING(clean_value, CHARINDEX('-', clean_value) + 1, 5) AS INT) ")
            .append("     ELSE NULL END AS opponent_points ")
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT LEFT(value, CHARINDEX('(', value + '(') - 1) AS clean_value ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append("    WHERE CHARINDEX('-', value) > 0 ")
            .append(") AS frame_data ")
            .append("WHERE m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ")
            .append("AND mps.frame_scores IS NOT NULL ")
            
            // ✅ Only include players with >= 20 matches in the same date range
            .append("AND mps.player_key IN ( ")
            .append("    SELECT mps2.player_key ")
            .append("    FROM match_player_stats mps2 ")
            .append("    JOIN match m2 ON mps2.match_key = m2.match_key ")
            .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
            .append("      AND m2.match_date >= :dateFrom ")
            .append("      AND m2.match_date <= :dateTo ")
            .append("    GROUP BY mps2.player_key ")
            .append("    HAVING COUNT(DISTINCT m2.match_key) >= 20 ")
            .append(") ")

            // End subquery
            .append(") AS frame_stats ")
            .append("WHERE player_points < opponent_points ")
            .append("GROUP BY player_key, player_name, country_name ")
            .append("ORDER BY avg_deficit ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getFiftyPlusBreaksInDeciders(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 50 ")
            .append("    THEN 1 ELSE 0 END) AS breaks_50_plus, ") // row[3]
            .append("COUNT(*) AS total_deciding_frames, ")        // row[4]
            .append("CAST(SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 50 ")
            .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage ") // row[5]
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT value AS last_frame, ")
            .append("           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") AS frames ")
            .append("WHERE rn = (SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';')) ") // ✅ Get last frame only
            .append("AND mps.frame_scores IS NOT NULL ")
            .append("AND ( ")
            .append("    SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") = (m.winner_score + m.loser_score) ") // ✅ Match went the distance (true decider)
            .append("AND (m.winner_score + m.loser_score) = (2 * m.winner_score - 1) ")
            .append("AND m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name ")
            .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getSeventyPlusBreaksInDeciders(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 70 ")
            .append("    THEN 1 ELSE 0 END) AS breaks_50_plus, ") // row[3]
            .append("COUNT(*) AS total_deciding_frames, ")        // row[4]
            .append("CAST(SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 70 ")
            .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage ") // row[5]
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT value AS last_frame, ")
            .append("           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") AS frames ")
            .append("WHERE rn = (SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';')) ") // ✅ Get last frame only
            .append("AND mps.frame_scores IS NOT NULL ")
            .append("AND ( ")
            .append("    SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") = (m.winner_score + m.loser_score) ") 
            .append("AND (m.winner_score + m.loser_score) = (2 * m.winner_score - 1) ")
            .append("AND m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name ")
            .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getHundredPlusBreaksInDeciders(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 100 ")
            .append("    THEN 1 ELSE 0 END) AS breaks_50_plus, ") // row[3]
            .append("COUNT(*) AS total_deciding_frames, ")        // row[4]
            .append("CAST(SUM(CASE WHEN ")
            .append("    CHARINDEX('(', last_frame) > 0 AND ")
            .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
            .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 100 ")
            .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage ") // row[5]
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON mps.match_key = m.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT value AS last_frame, ")
            .append("           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") AS frames ")
            .append("WHERE rn = (SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';')) ") // ✅ Get last frame only
            .append("AND mps.frame_scores IS NOT NULL ")
            .append("AND ( ")
            .append("    SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append(") = (m.winner_score + m.loser_score) ") 
            .append("AND (m.winner_score + m.loser_score) = (2 * m.winner_score - 1) ")
            .append("AND m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name ")
            .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getOpeningFrameWinPercentage(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
            .append("mps.player_key, ")
            .append("p.player_name, ")
            .append("p.country_name, ")
            .append("SUM(CASE WHEN TRY_CAST(LEFT(opening_frame, CHARINDEX('-', opening_frame) - 1) AS INT) > ")
            .append("              TRY_CAST(SUBSTRING(opening_frame, CHARINDEX('-', opening_frame) + 1, 5) AS INT) ")
            .append("     THEN 1 ELSE 0 END) AS opening_frames_won, ")
            .append("COUNT(*) AS total_opening_frames, ")
            .append("CAST(SUM(CASE WHEN TRY_CAST(LEFT(opening_frame, CHARINDEX('-', opening_frame) - 1) AS INT) > ")
            .append("                 TRY_CAST(SUBSTRING(opening_frame, CHARINDEX('-', opening_frame) + 1, 5) AS INT) ")
            .append("           THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS win_percentage ")
            .append("FROM match_player_stats mps ")
            .append("JOIN player p ON mps.player_key = p.player_key ")
            .append("JOIN match m ON m.match_key = mps.match_key ")
            .append("JOIN event e ON m.event_key = e.event_key ")
            .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
            .append("CROSS APPLY ( ")
            .append("    SELECT TOP 1 value AS opening_frame ")
            .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
            .append("    WHERE CHARINDEX('-', value) > 0 ")
            .append(") AS frame_data ")
            .append("WHERE m.match_date >= :dateFrom ")
            .append("AND m.match_date <= :dateTo ")
            .append("AND mps.frame_scores IS NOT NULL ")

            // ✅ Only players with >= 20 full matches in date range
            .append("AND mps.player_key IN ( ")
            .append("    SELECT mps2.player_key ")
            .append("    FROM match_player_stats mps2 ")
            .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
            .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
            .append("      AND m2.match_date >= :dateFrom AND m2.match_date <= :dateTo ")
            .append("    GROUP BY mps2.player_key ")
            .append("    HAVING COUNT(DISTINCT m2.match_key) >= 20 ")
            .append(") ")

            .append("GROUP BY mps.player_key, p.player_name, p.country_name ")
            .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    private List<Object[]> getOpeningTwoFrameWinPercentage(
        TournamentDTO tournament,
        Integer year,
        Integer eventKey,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean orderAsc,
        Integer topLimit) { 

        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT TOP ").append(topLimit).append(" ")
        .append("mps.player_key, ")
        .append("p.player_name, ")
        .append("p.country_name, ")
        // Count matches where both first AND second frames were won
        .append("SUM(CASE ")
        .append("  WHEN ")
        .append("    TRY_CAST(LEFT(opening_frame1, CHARINDEX('-', opening_frame1) - 1) AS INT) > ")
        .append("      TRY_CAST(SUBSTRING(opening_frame1, CHARINDEX('-', opening_frame1) + 1, 5) AS INT) ")
        .append("  AND ")
        .append("    TRY_CAST(LEFT(opening_frame2, CHARINDEX('-', opening_frame2) - 1) AS INT) > ")
        .append("      TRY_CAST(SUBSTRING(opening_frame2, CHARINDEX('-', opening_frame2) + 1, 5) AS INT) ")
        .append("  THEN 1 ELSE 0 END) AS opening_2_frames_won, ")
        // Denominator: matches with both frames present
        .append("COUNT(*) AS total_opening_2_frames, ")
        .append("CAST(SUM(CASE ")
        .append("    WHEN TRY_CAST(LEFT(opening_frame1, CHARINDEX('-', opening_frame1) - 1) AS INT) > ")
        .append("           TRY_CAST(SUBSTRING(opening_frame1, CHARINDEX('-', opening_frame1) + 1, 5) AS INT) ")
        .append("     AND TRY_CAST(LEFT(opening_frame2, CHARINDEX('-', opening_frame2) - 1) AS INT) > ")
        .append("           TRY_CAST(SUBSTRING(opening_frame2, CHARINDEX('-', opening_frame2) + 1, 5) AS INT) ")
        .append("  THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS win_percentage ")
        .append("FROM match_player_stats mps ")
        .append("JOIN player p ON mps.player_key = p.player_key ")
        .append("JOIN match m ON m.match_key = mps.match_key ")
        .append("JOIN event e ON m.event_key = e.event_key ")
        .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
        // Get first frame
        .append("CROSS APPLY ( ")
        .append("  SELECT TOP 1 value AS opening_frame1 ")
        .append("  FROM STRING_SPLIT(mps.frame_scores, ';') ")
        .append("  WHERE CHARINDEX('-', value) > 0 ")
        .append(") AS fd1 ")
        // Get second frame
        .append("CROSS APPLY ( ")
        .append("  SELECT TOP 1 value AS opening_frame2 ")
        .append("  FROM ( ")
        .append("    SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn ")
        .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
        .append("    WHERE CHARINDEX('-', value) > 0 ")
        .append("  ) x ")
        .append("  WHERE rn = 2 ")
        .append(") AS fd2 ")
        .append("WHERE m.match_date >= :dateFrom ")
        .append("AND m.match_date <= :dateTo ")
        .append("AND mps.frame_scores IS NOT NULL ")
        // Only completed matches
        .append("AND m.winner_key IS NOT NULL ")
        .append("AND m.loser_key  IS NOT NULL ")
        // Only players with >= 20 matches
        .append("AND mps.player_key IN ( ")
        .append("  SELECT mps2.player_key ")
        .append("  FROM match_player_stats mps2 ")
        .append("  JOIN match m2 ON m2.match_key = mps2.match_key ")
        .append("  WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
        .append("    AND m2.match_date >= :dateFrom AND m2.match_date <= :dateTo ")
        .append("  GROUP BY mps2.player_key ")
        .append("  HAVING COUNT(DISTINCT m2.match_key) >= 20 ")
        .append(") ")
        // Only matches where both frames are present
        .append("AND fd1.opening_frame1 IS NOT NULL ")
        .append("AND fd2.opening_frame2 IS NOT NULL ")
        .append("GROUP BY mps.player_key, p.player_name, p.country_name ")
        .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }


}
