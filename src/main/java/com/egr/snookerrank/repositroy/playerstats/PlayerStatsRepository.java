package com.egr.snookerrank.repositroy.playerstats;

import com.egr.snookerrank.dto.TournamentDTO;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

public interface PlayerStatsRepository {
    public final String BASE_QUERY          = " SELECT p.player_key,p.player_name,p.country_name,COUNT(*) tot_wins FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE e.event_date>= :dDateFrom  AND e.event_date<= :dDateTo ";
    public final String BASE_QUERY_CASE_50  = " SELECT p.player_key,p.player_name,p.country_name,SUM(pp.prize_money * e.conversion_rate) sum_prize_money FROM player p JOIN player_prize pp ON p.player_key=pp.player_key JOIN event e ON pp.event_key=e.event_key WHERE e.event_date>= :dDateFrom  AND e.event_date<= :dDateTo ";
    public  final String GROUP_BY = " GROUP BY p.player_key,p.player_name,p.country_name ORDER BY tot_wins DESC";
    public final String TOURNAMENT_WC = " AND (e.event_category='WC') ";
    public final String TOURNAMENT_3C = " AND (e.tournament_key IN (1,2,3)) ";
    public final String TOURNAMENT_ALL_RANK = "  AND (e.event_category<>'0' AND e.event_category<>'U' AND e.event_category<>'Q' AND e.event_category<>'Y') ";
    public final String TOUR_YEAR = "  AND p.player_key IN (SELECT player_key FROM player_pro_card WHERE year= :year  )";
    public final String TOUR_EVENT = "  AND (p.player_key IN (SELECT player1_key FROM fixture WHERE event_key= :event) OR p.player_key IN (SELECT player2_key FROM fixture WHERE event_key= :event) OR p.player_key IN (SELECT DISTINCT fo.player_key FROM fixture_odds fo JOIN fixture f ON fo.fixture_key=f.fixture_key WHERE f.event_key= :event";
    public final String RANK_KEY_51 = " AND pp.round_no IN (21) ";
    public final String RANK_KEY_52 = " AND pp.round_no IN (21,20) ";
    public final String RANK_KEY_53 = " AND pp.round_no IN (21,20,19) ";
    public final String RANK_KEY_54 = " AND pp.round_no IN (21,20,19,18) ";
    public final String GROUP_BY_CASE_50 =" GROUP BY p.player_key,p.player_name,p.country_name ORDER BY sum_prize_money DESC ";

    public final String SELECT_TOP = "SELECT TOP :top  p.player_key,p.player_name,p.country_name,";

    List<Object[]> findPlayersWithFilters(LocalDate dDateFrom, LocalDate dDateTo, Integer eventKey, TournamentDTO tournament, Integer year, Integer rankKey);
    List<Object[]> findPlayersWithStatsRankingFilters(String statType, String field1, String field2,TournamentDTO tournament,Integer year, Integer eventKey,LocalDate dateFrom, LocalDate dateTo, Integer minMatches, Boolean orderAsc, Integer topLimit);
    public PlayerStatsRepositoryImpl.MaxBreakStatsDTO getTotalMaxBreaks(Integer playerKey);

    }
