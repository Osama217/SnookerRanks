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

import java.util.ArrayList;
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
        rankings.put(7, "Tournament Wins");
        rankings.put(3, "Tournament Cashes");
        rankings.put(2, "Tournament Appearances");
        rankings.put(5, "All Time Rank Pts");
        rankings.put(10, "Player Success");
        rankings.put(4, "Country Success");
        rankings.put(11, "Matches Played");

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
        StringBuilder stats,temp_stats =null;
        String multiplier = "A".equals(statType) ? "*3" : "";
        StringBuilder query = new StringBuilder("SELECT p.player_key AS playerKey, p.player_name AS playerName, p.country_name AS countryName, ") ;
        boolean DAP = statType.equals("D") || statType.equals("A") || statType.equals("P");

        if(DAP){
            stats = new StringBuilder("CONVERT(float, SUM(mps." + field1 + "))" + (!multiplier.isEmpty() ? " * " + multiplier : "") + " / CONVERT(float, SUM(mps." + field2 + ")") ;
            temp_stats = new StringBuilder()
                    .append("'(' + CAST(SUM(mps.").append(field1).append(") AS VARCHAR) + '/' + CAST(SUM(mps.")
                    .append(field2).append(") AS VARCHAR) + ') ' + CAST(ROUND((")
                    .append("CONVERT(float, SUM(mps.").append(field1).append("))")
                    .append(!multiplier.isEmpty() ? " * " + multiplier : "")
                    .append(") / CONVERT(float, SUM(mps.").append(field2).append("))")
                    .append(statType.equals("P") ? " * 100" : "")
                    .append(", 2) AS VARCHAR ");
            query.append(" CAST (").append(temp_stats.toString()).append(" )AS VarChar) ").append(statType.equals("P") ? " + '%'" : "");


        }else if (statType.equals("X")){
            stats = new StringBuilder("MAX(CAST(mps.").append(field1).append(" AS FLOAT)");
            query.append(" Cast( ").append(stats.toString()).append(" )as varchar) ");
        }

        else if(statType.equals("%opening_frames_won")){
            stats = new StringBuilder("SUM(CAST(mps.").append(field1).append(" AS FLOAT) ") ;
            query.append(" Cast(").append(stats.toString()).append(")AS varchar) ");
        }
        
        query.append(" AS stats FROM player p JOIN match_player_stats mps ON p.player_key = mps.player_key JOIN match m ON mps.match_key = m.match_key JOIN event e ON m.event_key = e.event_key WHERE mps.").append(field1).append(" <> -1 ").append("AND e.tournament_key =").append(tournamentKey).append(" AND e.event_date >= '").append(dateFrom).append("' AND e.event_date <= '").append(dateTo).append("' GROUP BY p.player_key, p.player_name, p.country_name  ");
        if(DAP) {
            query.append(" HAVING SUM(CAST(mps.").append(field2).append(" AS FLOAT)) > 0 ");
        }
        if(null != stats)
            query.append("ORDER BY ").append(stats.toString()).append(" ) DESC");

        List<Object[]> results = entityManager.createNativeQuery(query.toString()).getResultList();

        List<PlayerTournamnetStatsDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            Integer playerKey = (row[0] != null) ? ((Number) row[0]).intValue() : null;
            String playerName = (String) row[1];
            String countryName = (String) row[2];
            String stats1 = (String) row[3];

            PlayerTournamnetStatsDTO dto = new PlayerTournamnetStatsDTO(playerKey, playerName, countryName, stats1);
            dtos.add(dto);
        }
        return dtos;
    }


}
