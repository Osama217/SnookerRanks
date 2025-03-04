package com.egr.snookerrank.service;

import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.repositroy.PlayerRepository;
import com.egr.snookerrank.utils.CommonUtilities;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<TopPlayersDTO> getTopPlayers(@RequestParam int count) {
        int limit = (count > 0) ? count : 10;  // Default to 10 if count is invalid
        TopPlayersDTO dto = new TopPlayersDTO();
        dto.getPlayerKey();
        return playerRepository.findTopPlayers(count)
                .stream()
                .map(obj -> {
                    Object[] row = (Object[]) obj; // Explicit cast to Object[]
                    return
                            new TopPlayersDTO(
                                    (Integer) row[0],  // player_key
                                    (String) row[1],   // player_name
                                    (String) row[2],   // country_name
                                    (Double) row[3]    // fdi
                            );
                }).collect(Collectors.toList());
    }

    public List<OrderOfMeritDTO> getOrderOfMerit(int monthsBack, int topCount, List<Integer> excludedPlayers) {
        LocalDate dateTo = LocalDate.now();
        LocalDate dateFrom = dateTo.minusMonths(monthsBack);
        if (excludedPlayers == null || excludedPlayers.isEmpty()) {
            excludedPlayers = null; // Empty list to avoid SQL issues
        }
        int effectiveYear = (dateTo.getMonthValue() == 1 && dateTo.getDayOfMonth() < 7) ? dateTo.getYear() - 1 : dateTo.getYear();

        List<Object[]> result = playerRepository.findOrderOfMerit(
                dateFrom.format(DateTimeFormatter.ISO_DATE),
                dateTo.format(DateTimeFormatter.ISO_DATE),
                topCount,
                effectiveYear,
                excludedPlayers
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return result.stream().map(row -> new OrderOfMeritDTO(
                (Integer) row[0],  // playerKey
                (String) row[1],   // playerName
                (String) row[2],   // countryName
                (Double) row[3],  // age
                null != row[4] ? ((Timestamp) row[4]).toLocalDateTime().format(formatter) : null,   // dob
                (Boolean) row[5],  // isWoman
                (Double) row[6],  // sumPrizeMoney
                (Integer) row[7],  // totalEvents
                (Integer) row[8]   // inProgress
        )).collect(Collectors.toList());
    }

    public StatsDTO getStats() {
        StatsDTO statsDTO= null;
        List<Object[]> result = playerRepository.fetchRanks();
        List<RanksDTO> ranksDTOList = result.stream().map(row -> new RanksDTO(
                (Integer) row[0],
                (String) row[1]
        )).collect(Collectors.toList());
        List<TournamentDTO> tournamentDTOS = Arrays.asList(TournamentDTO.values());
        List<Integer> years = CommonUtilities.generateYearList();
        statsDTO = new StatsDTO(ranksDTOList,tournamentDTOS,years);
        return  statsDTO;

    }
}
