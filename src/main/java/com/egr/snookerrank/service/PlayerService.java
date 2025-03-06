package com.egr.snookerrank.service;

import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.PlayerDetailsDTO;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.beans.PlayerPrizeStats;
import com.egr.snookerrank.model.RankText;
import com.egr.snookerrank.repositroy.PlayerRepository;
import com.egr.snookerrank.repositroy.RankTextRepository;
import com.egr.snookerrank.repositroy.playerstats.PlayerStatsRepository;
import com.egr.snookerrank.repositroy.playerstats.PlayerStatsRepositoryImpl;
import com.egr.snookerrank.utils.CommonUtilities;
import lombok.AllArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final RankTextRepository rankTextRepository;


    public List<TopPlayersDTO> getTopPlayers(@RequestParam int count) {
        int limit = (count > 0) ? count : 10;  // Default to 10 if count is invalid
        TopPlayersDTO dto = new TopPlayersDTO();
        AtomicInteger rankCounter = new AtomicInteger(1);
        return playerRepository.findTopPlayers(count)
                .stream()
                .map(obj -> {
                    Object[] row = (Object[]) obj; // Explicit cast to Object[]
                    return
                            new TopPlayersDTO(
                                    rankCounter.getAndIncrement(),
                                    (Integer) row[0],  // player_key
                                    (String) row[1],   // player_name
                                    (String) row[2],   // country_name
                                    row[3] != null ?
                                            new BigDecimal(((Number) row[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP) : null
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
        StatsDTO statsDTO = null;
        List<Object[]> result = playerRepository.fetchRanks();
        List<RanksDTO> ranksDTOList = result.stream().map(row -> new RanksDTO(
                (Integer) row[0],
                (String) row[1]
        )).collect(Collectors.toList());
        List<TournamentDTO> tournamentDTOS = Arrays.asList(TournamentDTO.values());
        List<Integer> years = CommonUtilities.generateYearList();
        statsDTO = new StatsDTO(ranksDTOList, tournamentDTOS, years);
        return statsDTO;

    }

    public List<PlayerStats> fetchPlayerStats(LocalDate dDateFrom, LocalDate dDateTo, Integer eventKey, TournamentDTO tournament, Integer year, Integer rankKey, Integer minMatches) {
        List<Object[]> result = null;
        if (rankKey.equals(50) || rankKey.equals(51) || rankKey.equals(52) || rankKey.equals(53) || rankKey.equals(54)) {
            result = playerStatsRepository.findPlayersWithFilters(dDateFrom, dDateTo, eventKey, tournament, year, rankKey);

        } else {
            RankText rankText = rankTextRepository.findByRankTextKey(rankKey);
            if (rankText != null && rankText.isMatchStat()) {
                result = playerStatsRepository.findPlayersWithStatsRankingFilters(rankText.getStatType(), rankText.getField1(), rankText.getField2(),
                        tournament, year, eventKey, dDateFrom, dDateTo, minMatches, rankText.isOrderAsc(), 500);
            }
        }
        List<PlayerStats> ranksDTOList = result.stream().map(row -> {
            if (row.length > 4) {
                return new PlayerStats(
                        (Integer) row[0],
                        (String) row[1],
                        (String) row[2],
                        row.length > 5 && row[5] != null ?
                                new BigDecimal(((Number) row[5]).doubleValue()).setScale(2, RoundingMode.HALF_UP)
                                : null,
                        (Number) row[3],
                        (Number) row[4]
                );
            } else {
                return new PlayerStats(
                        (Integer) row[0],
                        (String) row[1],
                        (String) row[2],
                        (Number) row[3]
                );
            }
        }).collect(Collectors.toList());
        return ranksDTOList;

    }

    public List<TopPlayersDTO> searchPlayer(String name) {
        String formattedSearchString = name.replace("_", " ");
        List<Player> playersList = playerRepository.findByPlayerNameContainingOrderByFdiDesc(formattedSearchString);
        List<TopPlayersDTO> playersDTOList = new ArrayList<>();
        if (playersList != null) {
            int rank = 1;
            for (Player player : playersList) {
                TopPlayersDTO dto = new TopPlayersDTO();
                BeanUtils.copyProperties(player, dto);
                dto.setRank(rank++);
                playersDTOList.add(dto);
            }
        }
        return playersDTOList;
    }

    public PlayerDetailsDTO playerDetails(Integer key, boolean isRanking) {
        PlayerDetailsDTO playerDetails = new PlayerDetailsDTO();
        PlayerDTO playerDTO = null;
        List<PlayerPrizeStatsDTO> playerPrizeStatsDTOList = null;

        Player player = playerRepository.findByPlayerKey(key);
        if (player != null) {

            playerDetails.setPlayer(mapPlayerDetails(player));
            //Going to fetch detials of Earns
            String filter = isRanking ? "Y" : "N";
            List<PlayerPrizeStats> playerPrizeStats = playerRepository.findPlayerPrizeStatistics(key, filter);
            if (playerPrizeStats != null) {
                playerPrizeStatsDTOList = new ArrayList<>();
                for (PlayerPrizeStats prizeStats : playerPrizeStats) {
                    PlayerPrizeStatsDTO dto = new PlayerPrizeStatsDTO();
                    BeanUtils.copyProperties(prizeStats, dto);
                    playerPrizeStatsDTOList.add(dto);
                }
                playerDetails.setPrizeStats(playerPrizeStatsDTOList);
            }



            return playerDetails;
        } else {
            throw new RuntimeException("Invalid player key");
        }
    }

    private PlayerDTO mapPlayerDetails(Player player) {
        PlayerDTO playerDTO = new PlayerDTO();
        BeanUtils.copyProperties(player, playerDTO);
        if (player.getBiogLink() != null) {
            playerDTO.setBiogLink(StringEscapeUtils.unescapeHtml4(player.getBiogLink()));
        }
        if (player.getBiogPictureLink() != null) {
            playerDTO.setBiogPictureLink(StringEscapeUtils.unescapeHtml4(player.getBiogPictureLink()));
        }

        //todo
        PlayerStatsRepositoryImpl.MaxBreakStatsDTO stats =playerStatsRepository.getTotalMaxBreaks(playerDTO.getPlayerKey());
        playerDTO.setCareer147s(stats.totalMaxBreaks());
        playerDTO.setCareerCenturies(stats.totalCenturyBreaks());

        return playerDTO;
    }


}
