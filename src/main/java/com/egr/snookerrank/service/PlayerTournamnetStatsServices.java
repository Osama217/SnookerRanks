package com.egr.snookerrank.service;

import com.egr.snookerrank.beans.PlayerTournamnetStatsDTO;
import com.egr.snookerrank.model.RankText;
import com.egr.snookerrank.repositroy.PlayerRepository;
import com.egr.snookerrank.repositroy.RankTextRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlayerTournamnetStatsServices {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private RankTextRepository rankTextRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public Map<Integer, String> getRankings() {
        Map<Integer, String> rankings = new LinkedHashMap<>();

        rankings.put(1, "Prize Money");
        rankings.put(2, "Tournament Wins");
        rankings.put(3, "Tournament Cashes");
        rankings.put(4, "Tournament Appearances");
        rankings.put(5, "All Time Rank Pts");
        rankings.put(6, "Player Success");
        rankings.put(7, "Country Success");
        rankings.put(8, "Matches Played");

        String sql = "SELECT rank_text_key, rank_name FROM rank_text WHERE is_match_stat = 1";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        int count = 9;
        for (Map<String, Object> row : results) {
            Integer key = (Integer) row.get("rank_text_key");
            String value = (String) row.get("rank_name");
            rankings.put(key, value);
        }

        return rankings;
    }

    public List<PlayerTournamnetStatsDTO> getStats(Integer tournamnetKey,Integer tournamnetStatsKey,String dateFrom,String dateTo) {
        String sEventSQL = " AND e.tournament_key=" + tournamnetKey + " AND e.event_date>= " + dateFrom + " AND e.event_date<=" + dateTo + " ";
        List<PlayerTournamnetStatsDTO> statsDTOS =null;
        switch (tournamnetStatsKey) {
            case 1:
                statsDTOS =  playerRepository.getPlayerPrizeSummary(tournamnetKey,dateFrom,dateTo);
                break;
            case 2:
                statsDTOS = playerRepository.getPlayerApplications(tournamnetKey,dateFrom,dateTo);
                break;

            case 11:
                statsDTOS = playerRepository.getPlayerMatches(tournamnetKey,dateFrom,dateTo);
                break;

            case 3:
                statsDTOS = playerRepository.getPlayerCashes(tournamnetKey,dateFrom,dateTo);
                break;

            case 6:
                statsDTOS = playerRepository.getWinnerAverage(tournamnetKey,dateFrom,dateTo);
                break;

            case 7:
                statsDTOS = playerRepository.getPlayerWins(tournamnetKey,dateFrom,dateTo);
                break;
            case 4:
                statsDTOS = playerRepository.getPlayerCountryStats(tournamnetKey,dateFrom,dateTo);
                break;

            case 10:
                statsDTOS = playerRepository.getPlayerSuccessStats(tournamnetKey,dateFrom,dateTo);
                break;
            default:
                RankText rankText = rankTextRepository.findByRankTextKey(tournamnetStatsKey);
                if(null != rankText) {

                    statsDTOS = getMatchStats(rankText.getStatType(), rankText.getField1(), rankText.getField2(), tournamnetKey, dateFrom, dateTo);
                }
                break;



        }
        return statsDTOS;
    }

    public List<PlayerTournamnetStatsDTO> getMatchStats(String statType,String field1, String field2,
                                                        Integer tournamentKey, String dateFrom, String dateTo) {
        String query = "SELECT p.player_key AS playerKey, p.player_name AS playerName, p.country_name AS countryName, " +
                "CASE WHEN :statType IN ('D', 'A', 'P') THEN SUM(CAST(mps." + field1 + " AS FLOAT)) " +
                "     ELSE MAX(CAST(mps." + field1 + " AS FLOAT)) END AS stats " +
                "FROM player p " +
                "JOIN match_player_stats mps ON p.player_key = mps.player_key " +
                "JOIN match m ON mps.match_key = m.match_key " +
                "JOIN event e ON m.event_key = e.event_key " +
                "WHERE mps." + field1 + " <> -1 " +
                "AND e.tournament_key = :tournamentKey " +
                "AND e.event_date >= :dateFrom " +
                "AND e.event_date <= :dateTo " +
                "GROUP BY p.player_key, p.player_name, p.country_name " ;
        if(statType.equals("D") ||statType.equals("A")||statType.equals("P") ) {
            query = query.concat("HAVING " +
                    "(CASE WHEN :statType IN ('D', 'A', 'P') THEN SUM(CAST(mps." + field2 + " AS FLOAT)) ELSE 0 END) > 0 "   // Conditional sum for field2
            );
        }
        query = query.concat("ORDER BY stats DESC");

        Query nativeQuery = entityManager.createNativeQuery(query, PlayerTournamnetStatsDTO.class);
        nativeQuery.setParameter("statType", statType);
        nativeQuery.setParameter("tournamentKey", tournamentKey);
        nativeQuery.setParameter("dateFrom", dateFrom);
        nativeQuery.setParameter("dateTo", dateTo);

        return nativeQuery.getResultList();
    }


}
