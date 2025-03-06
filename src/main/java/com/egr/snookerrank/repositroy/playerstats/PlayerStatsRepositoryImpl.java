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

        // Base Query
        StringBuilder sql = new StringBuilder("SELECT TOP " + topLimit + " p.player_key, p.player_name, p.country_name, ");
        // Determine main statistical field logic
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
}
