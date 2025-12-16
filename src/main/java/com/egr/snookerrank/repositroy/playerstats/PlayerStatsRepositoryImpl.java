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

    public record MaxBreakStatsDTO(Integer totalMaxBreaks, Integer totalCenturyBreaks) {
    }

    @Override
    public List<Object[]> findPlayersWithFilters(LocalDate dDateFrom, LocalDate dDateTo, Integer eventKey,
            TournamentDTO tournament, Integer year, Integer rankKey) {
        StringBuilder sql = new StringBuilder();
        if (rankKey.equals(50)) {
            sql.append(BASE_QUERY_CASE_50);
        } else {
            sql.append(BASE_QUERY);
        }
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);
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
        setAdditionalParameters(query, year, eventKey);
        return query.getResultList();
    }

    private static void setAdditionalFieldsInQuery(StringBuilder sql, TournamentDTO tournament, Integer year,
            Integer eventKey) {
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
    public List<Object[]> findPlayersWithStatsRankingFilters(String statType, String field1, String field2,
            TournamentDTO tournament, Integer year, Integer eventKey,
            LocalDate dateFrom, LocalDate dateTo, Integer minMatches, Boolean orderAsc, Integer topLimit) {

        // 2. Add logging at the start of the method
        System.out.println("\n=== QUERY PARAMETERS ===");
        System.out.println("statType: " + statType);
        System.out.println("minMatches: " + (minMatches != null ? minMatches : "null (will default to 20)"));
        System.out.println("dateFrom: " + dateFrom);
        System.out.println("dateTo: " + dateTo);
        System.out.println("topLimit: " + topLimit);
        // System.out.println("========================\n");

        // new stat types
        if ("%deciding_frames_won".equalsIgnoreCase(statType)) {
            return getDecidingFrameWinStats(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("av_deficit_frames_won".equalsIgnoreCase(statType)) {
            return getAverageDeficitFramesWon(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("av_deficit_frames_lost".equalsIgnoreCase(statType)) {
            return getAverageDeficitFramesLost(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("50_in_deciding".equalsIgnoreCase(statType)) {
            return getFiftyPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("70_in_deciding".equalsIgnoreCase(statType)) {
            return getSeventyPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("century_in_deciding".equalsIgnoreCase(statType)) {
            return getHundredPlusBreaksInDeciders(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("%matches_won".equalsIgnoreCase(statType)) {
            return getMatchWinPercentagePerPlayer(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit);
        }

        if ("%opening_frames_won".equalsIgnoreCase(statType)) {
            System.out.println(statType);
            System.out.println(year);
            return getOpeningFrameWinPercentage(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        if ("%opening_two_frames_won".equalsIgnoreCase(statType)) {
            System.out.println(statType);
            System.out.println(year);

            return getOpeningTwoFrameWinPercentage(tournament, year, eventKey, dateFrom, dateTo, orderAsc, topLimit,
                    minMatches);
        }

        // Base Query
        StringBuilder sql = new StringBuilder(
                "SELECT TOP " + topLimit + " p.player_key, p.player_name, p.country_name, ");

        // Determine main statistical field logic
        if (null == minMatches || minMatches <= 0) {
            minMatches = 10;
        }
        if ("D".equals(statType) || "A".equals(statType) || "P".equals(statType))
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
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

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
        setAdditionalParameters(query, year, eventKey);

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
        return new MaxBreakStatsDTO(((Number) result[0]).intValue(), ((Number) result[1]).intValue());
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
                .append("COUNT(DISTINCT CASE WHEN m.winner_key = mps.player_key THEN m.match_key END) AS matches_won, ") // ✅
                                                                                                                         // fixed
                .append("COUNT(DISTINCT m.match_key) AS total_matches, ") // ✅ fixed
                .append("CAST(COUNT(DISTINCT CASE WHEN m.winner_key = mps.player_key THEN m.match_key END) * 100.0 / COUNT(DISTINCT m.match_key) AS DECIMAL(5,2)) AS win_percentage ") // ✅
                                                                                                                                                                                       // fixed
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
    //////////////////////////////////////////////

    private List<Object[]> getDecidingFrameWinStats(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

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
                .append("CAST(COUNT(DISTINCT CASE WHEN mps.player_key = m.winner_key THEN m.match_key END) * 100.0 / NULLIF(COUNT(DISTINCT m.match_key),0) AS DECIMAL(5,2)) AS win_percentage, ")

                // ✅ Total matches played - from the JOIN
                .append("player_matches.total_matches AS total_matches_played ")

                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON m.match_key = mps.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")

                // ✅ JOIN to get total matches count
                .append("JOIN ( ")
                .append("    SELECT mps2.player_key, COUNT(DISTINCT m2.match_key) AS total_matches ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL ")
                .append("      AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") player_matches ON player_matches.player_key = mps.player_key ")

                // Filter: Only deciding frames
                .append("WHERE mps.frame_scores IS NOT NULL ")
                .append("AND ( ")
                .append("    SELECT COUNT(*) FROM STRING_SPLIT(mps.frame_scores, ';') ")
                .append(") = (m.winner_score + m.loser_score) ")
                .append("AND (m.winner_score + m.loser_score) = (2 * m.winner_score - 1) ")
                .append("AND m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ");

        // Optional filters for tournament, year, event
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name, player_matches.total_matches ")
                .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

    // Result array structure:
    // row[0] = player_key (Integer)
    // row[1] = player_name (String)
    // row[2] = country_name (String)
    // row[3] = deciders_won (Number)
    // row[4] = deciders_played (Number)
    // row[5] = win_percentage (Number)
    // row[6] = total_matches_played (Number)
    ////////////////////////////////////////////////////////

    private List<Object[]> getAverageDeficitFramesWon(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

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
                .append("AND mps.frame_scores IS NOT NULL ");

        // Add tournament, year, and event filters
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("AND mps.player_key IN ( ") // ✅ Only include players with >= 20 matches in the same date range
                .append("AND mps.player_key IN ( ")
                .append("    SELECT mps2.player_key ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON mps2.match_key = m2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") ")
                // End subquery
                .append(") AS frame_stats ")
                .append("WHERE player_points > opponent_points ")
                .append("GROUP BY player_key, player_name, country_name ")
                .append("ORDER BY avg_deficit ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);
        query.setParameter("topLimit", topLimit);

        return query.getResultList();
    }

    private List<Object[]> getAverageDeficitFramesLost(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

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
                .append("AND mps.frame_scores IS NOT NULL ");

        // Add tournament, year, and event filters
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("AND mps.player_key IN ( ") // ✅ Only include players with >= 20 matches in the same date range
                .append("AND mps.player_key IN ( ")
                .append("    SELECT mps2.player_key ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON mps2.match_key = m2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") ")

                // End subquery
                .append(") AS frame_stats ")
                .append("WHERE player_points < opponent_points ")
                .append("GROUP BY player_key, player_name, country_name ")
                .append("ORDER BY avg_deficit ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
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
            Integer topLimit,
            Integer minMatches) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TOP ").append(topLimit).append(" ")
                .append("mps.player_key, ")
                .append("p.player_name, ")
                .append("p.country_name, ")
                .append("SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 50 ")
                .append("    THEN 1 ELSE 0 END) AS breaks_50_plus, ")
                .append("COUNT(*) AS total_deciding_frames, ")
                .append("CAST(SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 50 ")
                .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage, ")
                .append("player_matches.total_matches AS total_matches_played ")
                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON mps.match_key = m.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                .append("JOIN ( ")
                .append("    SELECT mps2.player_key, COUNT(DISTINCT m2.match_key) AS total_matches ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL ")
                .append("      AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") player_matches ON player_matches.player_key = mps.player_key ")
                // ✅ FIXED: Properly extract LAST frame
                .append("CROSS APPLY ( ")
                .append("    SELECT REVERSE( ")
                .append("        LEFT( ")
                .append("            REVERSE(mps.frame_scores), ")
                .append("            CHARINDEX(';', REVERSE(mps.frame_scores) + ';') - 1 ")
                .append("        ) ")
                .append("    ) AS last_frame ")
                .append(") AS frames ")
                .append("WHERE mps.frame_scores IS NOT NULL ")
                .append("AND LEN(mps.frame_scores) > 0 ")
                // ✅ Verify frame count matches
                .append("AND (LEN(mps.frame_scores) - LEN(REPLACE(mps.frame_scores, ';', '')) + 1) = (m.winner_score + m.loser_score) ")
                // ✅ FIXED: Correct deciding frame logic
                .append("AND m.winner_score = m.loser_score + 1 ")
                .append("AND m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name, player_matches.total_matches ")
                .append("HAVING COUNT(*) >= 10 ") // Must have at least 10 deciding frames
                .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        // Print SQL to console
        System.out.println("\n=== GENERATED SQL QUERY ===");
        System.out.println(sql.toString());
        System.out.println("\n=== PARAMETERS ===");
        System.out.println("dateFrom: " + dateFrom);
        System.out.println("dateTo: " + dateTo);
        System.out.println("topLimit: " + topLimit);
        System.out.println("===========================\n");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);

        List<Object[]> results = query.getResultList();

        // Print results
        System.out.println("=== QUERY RESULTS ===");
        System.out.println("Total players found: " + results.size());
        System.out.println("\n" + String.format("%-12s %-30s %-15s %-12s %-18s %-12s %-15s",
                "Player Key", "Player Name", "Country", "50+ Breaks", "Deciding Frames", "Break %", "Total Matches"));
        System.out.println("-".repeat(125));

        for (Object[] row : results) {
            Integer playerKey = row[0] != null ? ((Number) row[0]).intValue() : null;
            String playerName = row[1] != null ? row[1].toString() : "N/A";
            String country = row[2] != null ? row[2].toString() : "N/A";
            Integer breaks50Plus = row[3] != null ? ((Number) row[3]).intValue() : 0;
            Integer decidingFrames = row[4] != null ? ((Number) row[4]).intValue() : 0;
            String breakPercentage = row[5] != null ? row[5].toString() : "0.00";
            Integer totalMatches = row[6] != null ? ((Number) row[6]).intValue() : 0;

            System.out.println(String.format("%-12s %-30s %-15s %-12d %-18d %-12s %-15d",
                    playerKey,
                    playerName.length() > 28 ? playerName.substring(0, 28) + ".." : playerName,
                    country.length() > 13 ? country.substring(0, 13) + ".." : country,
                    breaks50Plus,
                    decidingFrames,
                    breakPercentage + "%",
                    totalMatches));
        }

        System.out.println("-".repeat(125));
        System.out.println("=== END OF RESULTS ===\n");

        return results;
    }

    private List<Object[]> getSeventyPlusBreaksInDeciders(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TOP ").append(topLimit).append(" ")
                .append("mps.player_key, ")
                .append("p.player_name, ")
                .append("p.country_name, ")
                .append("SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 70 ")
                .append("    THEN 1 ELSE 0 END) AS breaks_70_plus, ")
                .append("COUNT(*) AS total_deciding_frames, ")
                .append("CAST(SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 70 ")
                .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage, ")
                .append("player_matches.total_matches AS total_matches_played ")
                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON mps.match_key = m.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                // ✅ JOIN to get total matches count
                .append("JOIN ( ")
                .append("    SELECT mps2.player_key, COUNT(DISTINCT m2.match_key) AS total_matches ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL ")
                .append("      AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") player_matches ON player_matches.player_key = mps.player_key ")
                // ✅ FIXED: Properly extract LAST frame using REVERSE
                .append("CROSS APPLY ( ")
                .append("    SELECT REVERSE( ")
                .append("        LEFT( ")
                .append("            REVERSE(mps.frame_scores), ")
                .append("            CHARINDEX(';', REVERSE(mps.frame_scores) + ';') - 1 ")
                .append("        ) ")
                .append("    ) AS last_frame ")
                .append(") AS frames ")
                .append("WHERE mps.frame_scores IS NOT NULL ")
                .append("AND LEN(mps.frame_scores) > 0 ")
                // ✅ Verify frame count matches
                .append("AND (LEN(mps.frame_scores) - LEN(REPLACE(mps.frame_scores, ';', '')) + 1) = (m.winner_score + m.loser_score) ")
                // ✅ FIXED: Correct deciding frame logic
                .append("AND m.winner_score = m.loser_score + 1 ")
                .append("AND m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name, player_matches.total_matches ")
                .append("HAVING COUNT(*) >= 10 ") // ✅ ADDED: Must have at least 10 deciding frames
                .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        // Print SQL to console for validation
        System.out.println("\n=== GENERATED SQL QUERY (70+ Breaks) ===");
        System.out.println(sql.toString());
        System.out.println("\n=== PARAMETERS ===");
        System.out.println("dateFrom: " + dateFrom);
        System.out.println("dateTo: " + dateTo);
        System.out.println("topLimit: " + topLimit);
        System.out.println("orderAsc: " + orderAsc);
        System.out.println("===========================\n");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);

        List<Object[]> results = query.getResultList();

        // Print results with deciding frames count
        System.out.println("=== QUERY RESULTS (70+ Breaks in Deciders) ===");
        System.out.println("Total players found: " + results.size());
        System.out.println("\n" + String.format("%-12s %-30s %-15s %-12s %-18s %-12s %-15s",
                "Player Key", "Player Name", "Country", "70+ Breaks", "Deciding Frames", "Break %", "Total Matches"));
        System.out.println("-".repeat(125));

        for (Object[] row : results) {
            Integer playerKey = row[0] != null ? ((Number) row[0]).intValue() : null;
            String playerName = row[1] != null ? row[1].toString() : "N/A";
            String country = row[2] != null ? row[2].toString() : "N/A";
            Integer breaks70Plus = row[3] != null ? ((Number) row[3]).intValue() : 0;
            Integer decidingFrames = row[4] != null ? ((Number) row[4]).intValue() : 0;
            String breakPercentage = row[5] != null ? row[5].toString() : "0.00";
            Integer totalMatches = row[6] != null ? ((Number) row[6]).intValue() : 0;

            System.out.println(String.format("%-12s %-30s %-15s %-12d %-18d %-12s %-15d",
                    playerKey,
                    playerName.length() > 28 ? playerName.substring(0, 28) + ".." : playerName,
                    country.length() > 13 ? country.substring(0, 13) + ".." : country,
                    breaks70Plus,
                    decidingFrames,
                    breakPercentage + "%",
                    totalMatches));
        }

        System.out.println("-".repeat(125));
        System.out.println("=== END OF RESULTS ===\n");

        return results;
    }

    private List<Object[]> getHundredPlusBreaksInDeciders(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TOP ").append(topLimit).append(" ")
                .append("mps.player_key, ")
                .append("p.player_name, ")
                .append("p.country_name, ")
                .append("SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 100 ")
                .append("    THEN 1 ELSE 0 END) AS breaks_100_plus, ")
                .append("COUNT(*) AS total_deciding_frames, ")
                .append("CAST(SUM(CASE WHEN ")
                .append("    CHARINDEX('(', last_frame) > 0 AND ")
                .append("    TRY_CAST(SUBSTRING(last_frame, CHARINDEX('(', last_frame) + 1, ")
                .append("        CHARINDEX(')', last_frame) - CHARINDEX('(', last_frame) - 1) AS INT) >= 100 ")
                .append("    THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS break_percentage, ")
                .append("player_matches.total_matches AS total_matches_played ")
                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON mps.match_key = m.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                // ✅ JOIN to get total matches count
                .append("JOIN ( ")
                .append("    SELECT mps2.player_key, COUNT(DISTINCT m2.match_key) AS total_matches ")
                .append("    FROM match_player_stats mps2 ")
                .append("    JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append("    WHERE m2.winner_key IS NOT NULL ")
                .append("      AND m2.loser_key IS NOT NULL ")
                .append("      AND m2.match_date >= :dateFrom ")
                .append("      AND m2.match_date <= :dateTo ")
                .append("    GROUP BY mps2.player_key ")
                .append("    HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") player_matches ON player_matches.player_key = mps.player_key ")
                // ✅ FIXED: Properly extract LAST frame using REVERSE
                .append("CROSS APPLY ( ")
                .append("    SELECT REVERSE( ")
                .append("        LEFT( ")
                .append("            REVERSE(mps.frame_scores), ")
                .append("            CHARINDEX(';', REVERSE(mps.frame_scores) + ';') - 1 ")
                .append("        ) ")
                .append("    ) AS last_frame ")
                .append(") AS frames ")
                .append("WHERE mps.frame_scores IS NOT NULL ")
                .append("AND LEN(mps.frame_scores) > 0 ")
                // ✅ Verify frame count matches
                .append("AND (LEN(mps.frame_scores) - LEN(REPLACE(mps.frame_scores, ';', '')) + 1) = (m.winner_score + m.loser_score) ")
                // ✅ FIXED: Correct deciding frame logic
                .append("AND m.winner_score = m.loser_score + 1 ")
                .append("AND m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ");

        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("GROUP BY mps.player_key, p.player_name, p.country_name, player_matches.total_matches ")
                .append("HAVING COUNT(*) >= 10 ") // Must have at least 10 deciding frames
                .append("ORDER BY break_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        // Print SQL to console
        System.out.println("\n=== GENERATED SQL QUERY (100+ Breaks) ===");
        System.out.println(sql.toString());
        System.out.println("\n=== PARAMETERS ===");
        System.out.println("dateFrom: " + dateFrom);
        System.out.println("dateTo: " + dateTo);
        System.out.println("topLimit: " + topLimit);
        System.out.println("===========================\n");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);

        List<Object[]> results = query.getResultList();

        // Print results
        System.out.println("=== QUERY RESULTS (100+ Breaks in Deciders) ===");
        System.out.println("Total players found: " + results.size());
        System.out.println("\n" + String.format("%-12s %-30s %-15s %-12s %-18s %-12s %-15s",
                "Player Key", "Player Name", "Country", "100+ Breaks", "Deciding Frames", "Break %", "Total Matches"));
        System.out.println("-".repeat(125));

        for (Object[] row : results) {
            Integer playerKey = row[0] != null ? ((Number) row[0]).intValue() : null;
            String playerName = row[1] != null ? row[1].toString() : "N/A";
            String country = row[2] != null ? row[2].toString() : "N/A";
            Integer breaks100Plus = row[3] != null ? ((Number) row[3]).intValue() : 0;
            Integer decidingFrames = row[4] != null ? ((Number) row[4]).intValue() : 0;
            String breakPercentage = row[5] != null ? row[5].toString() : "0.00";
            Integer totalMatches = row[6] != null ? ((Number) row[6]).intValue() : 0;

            System.out.println(String.format("%-12s %-30s %-15s %-12d %-18d %-12s %-15d",
                    playerKey,
                    playerName.length() > 28 ? playerName.substring(0, 28) + ".." : playerName,
                    country.length() > 13 ? country.substring(0, 13) + ".." : country,
                    breaks100Plus,
                    decidingFrames,
                    breakPercentage + "%",
                    totalMatches));
        }

        System.out.println("-".repeat(125));
        System.out.println("=== END OF RESULTS ===\n");

        return results;
    }

    private List<Object[]> getOpeningFrameWinPercentage(
            TournamentDTO tournament,
            Integer year,
            Integer eventKey,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean orderAsc,
            Integer topLimit,
            Integer minMatches) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
                .append("mps.player_key, ")
                .append("p.player_name, ")
                .append("p.country_name, ")
                .append("SUM(CASE WHEN scores.player_score > scores.opp_score THEN 1 ELSE 0 END) AS opening_frames_won, ")
                .append("COUNT(DISTINCT CASE WHEN scores.player_score IS NOT NULL THEN mps.match_key END) AS total_opening_frames, ")
                .append("CAST(SUM(CASE WHEN scores.player_score > scores.opp_score THEN 1 ELSE 0 END) * 100.0 / ")
                .append(" NULLIF(COUNT(DISTINCT CASE WHEN scores.player_score IS NOT NULL THEN mps.match_key END), 0) AS DECIMAL(5,2)) AS win_percentage ")
                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON m.match_key = mps.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                // Extract first frame before ';'
                .append("CROSS APPLY ( ")
                .append(" SELECT CASE ")
                .append("  WHEN mps.frame_scores IS NULL OR LTRIM(RTRIM(mps.frame_scores)) = '' THEN NULL ")
                .append("  WHEN CHARINDEX(';', mps.frame_scores) > 0 ")
                .append("   THEN LEFT(mps.frame_scores, CHARINDEX(';', mps.frame_scores) - 1) ")
                .append("  ELSE mps.frame_scores ")
                .append(" END AS opening_frame_raw ")
                .append(") AS raw ")
                // Remove parentheses content: 116(71)-7 becomes 116-7
                .append("CROSS APPLY ( ")
                .append(" SELECT CASE ")
                .append("  WHEN raw.opening_frame_raw IS NULL THEN NULL ")
                .append("  WHEN CHARINDEX('(', raw.opening_frame_raw) = 0 THEN raw.opening_frame_raw ")
                .append("  ELSE LTRIM(RTRIM( ")
                .append("   LEFT(raw.opening_frame_raw, CHARINDEX('(', raw.opening_frame_raw) - 1) + ")
                .append("   SUBSTRING(raw.opening_frame_raw, CHARINDEX(')', raw.opening_frame_raw) + 1, LEN(raw.opening_frame_raw)) ")
                .append("  )) ")
                .append(" END AS opening_frame_clean ")
                .append(") AS cleaned ")
                // Parse player and opponent scores
                .append("CROSS APPLY ( ")
                .append(" SELECT ")
                .append("  CASE WHEN cleaned.opening_frame_clean IS NOT NULL ")
                .append("   AND CHARINDEX('-', cleaned.opening_frame_clean) > 0 ")
                .append("   THEN TRY_CAST(LTRIM(RTRIM(LEFT(cleaned.opening_frame_clean, CHARINDEX('-', cleaned.opening_frame_clean) - 1))) AS INT) ")
                .append("   ELSE NULL END AS player_score, ")
                .append("  CASE WHEN cleaned.opening_frame_clean IS NOT NULL ")
                .append("   AND CHARINDEX('-', cleaned.opening_frame_clean) > 0 ")
                .append("   THEN TRY_CAST(LTRIM(RTRIM(SUBSTRING(cleaned.opening_frame_clean, CHARINDEX('-', cleaned.opening_frame_clean) + 1, 100))) AS INT) ")
                .append("   ELSE NULL END AS opp_score ")
                .append(") AS scores ")
                .append("WHERE m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ")
                .append("AND m.winner_key IS NOT NULL ");

        // Add tournament, year, and event filters
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("AND mps.player_key IN ( ")
                .append(" SELECT mps2.player_key ")
                .append(" FROM match_player_stats mps2 ")
                .append(" JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append(" WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
                .append(" AND m2.match_date >= :dateFrom AND m2.match_date <= :dateTo ")
                .append(" GROUP BY mps2.player_key ")
                .append(" HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") ")
                .append("GROUP BY mps.player_key, p.player_name, p.country_name ")
                .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches); // ADD THIS

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
            Integer topLimit,
            Integer minMatches) {

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT TOP ").append(topLimit).append(" ")
                .append("mps.player_key, ")
                .append("p.player_name, ")
                .append("p.country_name, ")
                // Count matches where both first AND second frames were won
                .append("SUM(CASE ")
                .append("  WHEN frame1_player_score > frame1_opp_score ")
                .append("   AND frame2_player_score > frame2_opp_score ")
                .append("  THEN 1 ELSE 0 END) AS opening_2_frames_won, ")
                // Denominator: matches with both frames successfully parsed
                .append("COUNT(DISTINCT mps.match_key) AS total_opening_2_frames, ")
                .append("CAST(SUM(CASE ")
                .append("  WHEN frame1_player_score > frame1_opp_score ")
                .append("   AND frame2_player_score > frame2_opp_score ")
                .append("  THEN 1 ELSE 0 END) * 100.0 / COUNT(DISTINCT mps.match_key) AS DECIMAL(5,2)) AS win_percentage ")
                .append("FROM match_player_stats mps ")
                .append("JOIN player p ON mps.player_key = p.player_key ")
                .append("JOIN match m ON m.match_key = mps.match_key ")
                .append("JOIN event e ON m.event_key = e.event_key ")
                .append("JOIN tournament t ON e.tournament_key = t.tournament_key ")
                // Get first frame and clean it
                .append("CROSS APPLY ( ")
                .append("  SELECT TOP 1 ")
                .append("    CASE ")
                .append("      WHEN CHARINDEX('(', value) > 0 ")
                .append("      THEN LTRIM(RTRIM( ")
                .append("        LEFT(value, CHARINDEX('(', value) - 1) + ")
                .append("        SUBSTRING(value, CHARINDEX(')', value) + 1, LEN(value)) ")
                .append("      )) ")
                .append("      ELSE value ")
                .append("    END AS opening_frame1 ")
                .append("  FROM STRING_SPLIT(mps.frame_scores, ';') ")
                .append("  WHERE CHARINDEX('-', value) > 0 ")
                .append(") AS fd1 ")
                // Get second frame and clean it
                .append("CROSS APPLY ( ")
                .append("  SELECT TOP 1 ")
                .append("    CASE ")
                .append("      WHEN CHARINDEX('(', value) > 0 ")
                .append("      THEN LTRIM(RTRIM( ")
                .append("        LEFT(value, CHARINDEX('(', value) - 1) + ")
                .append("        SUBSTRING(value, CHARINDEX(')', value) + 1, LEN(value)) ")
                .append("      )) ")
                .append("      ELSE value ")
                .append("    END AS opening_frame2 ")
                .append("  FROM ( ")
                .append("    SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn ")
                .append("    FROM STRING_SPLIT(mps.frame_scores, ';') ")
                .append("    WHERE CHARINDEX('-', value) > 0 ")
                .append("  ) x ")
                .append("  WHERE rn = 2 ")
                .append(") AS fd2 ")
                // Parse frame 1 scores
                .append("CROSS APPLY ( ")
                .append("  SELECT ")
                .append("    TRY_CAST(LTRIM(RTRIM(LEFT(fd1.opening_frame1, CHARINDEX('-', fd1.opening_frame1) - 1))) AS INT) AS frame1_player_score, ")
                .append("    TRY_CAST(LTRIM(RTRIM(SUBSTRING(fd1.opening_frame1, CHARINDEX('-', fd1.opening_frame1) + 1, 100))) AS INT) AS frame1_opp_score ")
                .append(") AS parsed1 ")
                // Parse frame 2 scores
                .append("CROSS APPLY ( ")
                .append("  SELECT ")
                .append("    TRY_CAST(LTRIM(RTRIM(LEFT(fd2.opening_frame2, CHARINDEX('-', fd2.opening_frame2) - 1))) AS INT) AS frame2_player_score, ")
                .append("    TRY_CAST(LTRIM(RTRIM(SUBSTRING(fd2.opening_frame2, CHARINDEX('-', fd2.opening_frame2) + 1, 100))) AS INT) AS frame2_opp_score ")
                .append(") AS parsed2 ")
                .append("WHERE m.match_date >= :dateFrom ")
                .append("AND m.match_date <= :dateTo ")
                .append("AND mps.frame_scores IS NOT NULL ")
                // Only completed matches
                .append("AND m.winner_key IS NOT NULL ")
                .append("AND m.loser_key IS NOT NULL ");

        // Add tournament, year, and event filters
        setAdditionalFieldsInQuery(sql, tournament, year, eventKey);

        sql.append("AND mps.player_key IN ( ") // Only players with >= 30 matches
                .append("  SELECT mps2.player_key ")
                .append("  FROM match_player_stats mps2 ")
                .append("  JOIN match m2 ON m2.match_key = mps2.match_key ")
                .append("  WHERE m2.winner_key IS NOT NULL AND m2.loser_key IS NOT NULL ")
                .append("    AND m2.match_date >= :dateFrom AND m2.match_date <= :dateTo ")
                .append("  GROUP BY mps2.player_key ")
                .append("  HAVING COUNT(DISTINCT m2.match_key) >= :minMatches ")
                .append(") ")
                // Only matches where both frames are present AND successfully parsed
                .append("AND fd1.opening_frame1 IS NOT NULL ")
                .append("AND fd2.opening_frame2 IS NOT NULL ")
                .append("AND parsed1.frame1_player_score IS NOT NULL ")
                .append("AND parsed1.frame1_opp_score IS NOT NULL ")
                .append("AND parsed2.frame2_player_score IS NOT NULL ")
                .append("AND parsed2.frame2_opp_score IS NOT NULL ")
                .append("GROUP BY mps.player_key, p.player_name, p.country_name ")
                .append("ORDER BY win_percentage ").append(Boolean.TRUE.equals(orderAsc) ? "ASC" : "DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);
        query.setParameter("minMatches", minMatches);
        setAdditionalParameters(query, year, eventKey);

        return query.getResultList();
    }

}
