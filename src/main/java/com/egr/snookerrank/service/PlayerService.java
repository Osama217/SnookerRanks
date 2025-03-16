package com.egr.snookerrank.service;

import com.egr.snookerrank.beans.*;
import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.*;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.model.RankText;
import com.egr.snookerrank.model.TournamnetStats;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
            //
            List<PlayerTournamentDTO> playerTournamnetsList = playerRepository.findTournamentsByPlayer(key);
            if (null != playerTournamnetsList && !playerTournamnetsList.isEmpty()) {
                List<CompletableFuture<Void>> futures = playerTournamnetsList.stream()
                        .map(playerTournamentDTO -> CompletableFuture.runAsync(() -> {
                            if (!playerTournamentDTO.getTournamentName().isEmpty()) {
                                List<Integer> years = playerRepository.findEventDates(key, playerTournamentDTO.getTournamentKey(), playerTournamentDTO.getRoundNo());
                                playerTournamentDTO.setYears(years);
                            }
                        })).toList();
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                playerDetails.setBestMajorResults(playerTournamnetsList);
            }
            List<TournamentEventDTO> tournamentEvents = playerRepository.findTournamentEvents(key);
            playerDetails.setOtherTournamentEvents(tournamentEvents);
            //Going to fetch detials of Earns
            String filter = isRanking ? "Y" : "N";
            List<PlayerPrizeStats> playerPrizeStats = playerRepository.findPlayerPrizeStatistics(key, filter);
            if (playerPrizeStats != null) {
                playerPrizeStatsDTOList = new ArrayList<>();
                for (PlayerPrizeStats prizeStats : playerPrizeStats) {
                    PlayerPrizeStatsDTO dto = new PlayerPrizeStatsDTO();
                    BeanUtils.copyProperties(prizeStats, dto);
                    if (null != dto.getPrizePerEvent()) {
                        dto.setPrizePerEvent(BigDecimal.valueOf(dto.getPrizePerEvent().doubleValue())
                                .setScale(2, RoundingMode.HALF_UP));
                    }
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

        PlayerStatsRepositoryImpl.MaxBreakStatsDTO stats = playerStatsRepository.getTotalMaxBreaks(playerDTO.getPlayerKey());
        playerDTO.setCareer147s(stats.totalMaxBreaks());
        playerDTO.setCareerCenturies(stats.totalCenturyBreaks());

        return playerDTO;
    }


    public PlayerAdditionalDetailsDTO annualWinLoss(Integer key) {
        String formattedPercentage = "100%";
        PlayerAdditionalDetailsDTO playerAdditionalDetailsDTO = new PlayerAdditionalDetailsDTO();
        Player player = playerRepository.findByPlayerKey(key);
        if (player != null) {
            List<AnnualWinLoss> annualWinLossList = playerRepository.getAnnualWinLossRecords(key);
            if (!annualWinLossList.isEmpty()) {
                List<AnnualWinLossDTO> annualWinLossDTOList = new ArrayList<>();
                for (AnnualWinLoss annualWinLoss : annualWinLossList) {
                    if (null != annualWinLoss.getLosses() && annualWinLoss.getLosses() != 0) {
                        double percentage = ((double) annualWinLoss.getWins() / annualWinLoss.getMatches()) * 100;
                        formattedPercentage = String.format("%.2f", percentage) + "%";
                    }
                    AnnualWinLossDTO dto = AnnualWinLossDTO.builder()
                            .year(annualWinLoss.getYear())
                            .winsByLosses(annualWinLoss.getWins() + "/" + annualWinLoss.getLosses())
                            .legsWonByLegsLost(annualWinLoss.getLegsWon() + "/" + annualWinLoss.getLegsLost())
                            .winByLossPercentage(formattedPercentage)
                            .build();
                    annualWinLossDTOList.add(dto);
                }
                playerAdditionalDetailsDTO.setAnnualWinLoss(annualWinLossDTOList);
            }

        } else {
            throw new RuntimeException("Player not found with this key");
        }
        return playerAdditionalDetailsDTO;
    }

    public TournamentStatsDTO getTournamentStats(TournamnetStatsOption tournamnetStatsOption, Integer key) {
        TournamentStatsDTO tournamentStatsDTO = new TournamentStatsDTO();
        if (TournamnetStatsOption.RESULTS.equals(tournamnetStatsOption)) {
            List<PlayerMatchTournamentDTO> matchPrize = playerRepository.findMatchesByPlayer(key);
            if (!matchPrize.isEmpty()) {
                matchPrize.forEach(match -> {
                            match.setScore(match.getLoserScore() + " V " + match.getWinnerScore());
                            if (match.getWinnerKey().equals(key))
                                match.setResult("Won");
                            else
                                match.setResult("Lost");
                        }
                );
            }
            tournamentStatsDTO.setMatches(matchPrize);
        } else if (TournamnetStatsOption.PRIZE.equals(tournamnetStatsOption)) {
            List<PlayerPrizeTournamentDTO> playerPrize = playerRepository.findPlayerPrizes(key);
            tournamentStatsDTO.setPrizes(playerPrize);

        }
        return tournamentStatsDTO;
    }

    public EventResultsDTO getEventResults(Integer eventKey) {
        String tournamnetName = null, eventYear, tournamnetKey, venue, tournamentCompleteName = "";
        Integer tvChannel;
        Boolean hasSeed;
        List<PrizeFundsDTO> prizeFundsDTOList = null;
        Object[] eventDetail = null;
        List<Object[]> eventDetails = playerRepository.getEventTitle(eventKey);
        if (null != eventDetails && !eventDetails.isEmpty()) {
            eventDetail = eventDetails.getFirst();
            tournamnetName = (String) eventDetail[0];
            eventYear = String.valueOf(eventDetail[1]);
            hasSeed = (Boolean) eventDetail[2];
            tournamnetKey = String.valueOf(eventDetail[3]);
            tvChannel = (Integer) eventDetail[4];
            venue = (String) eventDetail[5];
            tournamentCompleteName = eventYear + " " + tournamnetName + " Results";
        }
        List<MatchResults> matchResults = playerRepository.getMatchResult(eventKey);
        List<PrizeFund> prizeFunds = playerRepository.getPrizeFunds(eventKey);
        if (null != matchResults && null != prizeFunds) {
            prizeFundsDTOList = prizeFunds.stream()
                    .map(prize -> new PrizeFundsDTO(
                            prize.getRoundName(),
                            prize.getRoundNo(),
                            prize.getPrizeMoney(),
                            prize.getNumberOfMatches(),
                            prize.getCountryName(),
                            prize.getIsLeague(),
                            prize.getIsGroup(),
                            matchResults.stream()
                                    .filter(match -> match.getRoundNo().equals(prize.getRoundNo())) // Match roundNo
                                    .collect(Collectors.toList()) // Collect matched results
                    ))
                    .collect(Collectors.toList());
        }
        return new EventResultsDTO(tournamentCompleteName, prizeFundsDTOList);


    }

    public List<EventListDTO> getEventList(Integer year) {
        List<EventListDTO> eventList = playerRepository.getEventList(year);
        return eventList;
    }

    public EventResultsDTO getEventPrizeFund(Integer eventKey) {
        String tournamnetName = null, eventYear, tournamentCompleteName = "";
        Object[] eventDetail = null;
        List<Object[]> eventDetails = playerRepository.getEventTitle(eventKey);
        if (null != eventDetails && !eventDetails.isEmpty()) {
            eventDetail = eventDetails.getFirst();
            tournamnetName = (String) eventDetail[0];
            eventYear = String.valueOf(eventDetail[1]);
            tournamentCompleteName = eventYear + " " + tournamnetName + " Results";
        }
        List<PrizeFundsDTO> list = playerRepository.getEventPrizeFund(eventKey);
        return new EventResultsDTO(tournamentCompleteName, list);

    }

    public List<MatchResultDTO> getTOurnamentDetals(Integer tournamentKey) {
        List<MatchResultDTO> result = playerRepository.getTournamentDetails(tournamentKey);
        result.forEach(match -> {
            match.setScore(match.getWinnerScore() + " V " + match.getLoserScore());
        });
        return result;

    }

    public List<TournamnetStatsSummaryDTO> tournamentStatsSummary(Integer tournamentKey) {
        LinkedHashMap<String, TournamnetStatsSummaryDTO> statMap = new LinkedHashMap<>();
        Map<String,Integer> highest = new LinkedHashMap<>();
        String name = playerRepository.getTournamentName(tournamentKey);
        if (null != name && !name.isEmpty()) {
            name = name + " Records";
            List<TournamnetStats> stats = playerRepository.getMostWins(tournamentKey);
            stats.addAll(playerRepository.getMostFinals(tournamentKey));
            stats.addAll(playerRepository.getMostSemis(tournamentKey));
            stats.addAll(playerRepository.getMostQuaters(tournamentKey));
            stats.addAll(playerRepository.getMostAppearances(tournamentKey));
            stats.addAll(playerRepository.getYoungestWinner(tournamentKey));
            stats.addAll(playerRepository.getOldestWinner(tournamentKey));
            stats.addAll(playerRepository.getMostCenturyinTour(tournamentKey));
            stats.addAll(playerRepository.getMostCenturyinMatch(tournamentKey));
            stats.forEach(stat->{
                highest.putIfAbsent(stat.getRoundLabel(),0);
                Integer max = highest.get(stat.getRoundLabel());
                if(stat.getCount()>max){
                    highest.put(stat.getRoundLabel(),stat.getCount());
                }
            });


            stats.forEach(
                    stat -> {
                        statMap.putIfAbsent(stat.getRoundLabel(), new TournamnetStatsSummaryDTO(stat.getRoundLabel(), ""));
                        TournamnetStatsSummaryDTO statsDTO = statMap.get(stat.getRoundLabel());
                        Integer max = highest.get(stat.getRoundLabel());
                        switch (stat.getRoundLabel()) {
                            case "Most Wins", "Most Finals", "Most Quarter Finals", "Most Semi Finals"
                                 ,"Most Appearances", "Most Century Breaks in a Match" -> {
                                if(Objects.equals(stat.getCount(), max)) {
                                    if (statsDTO.getAmount().isEmpty()) {
                                        statsDTO.setAmount(stat.getCount() + " by " + stat.getPlayerName());
                                    } else {
                                        statsDTO.setAmount(statsDTO.getAmount() + " , " + stat.getPlayerName());
                                    }
                                }
                            }
                            case "Most Century Breaks in a Tournament" ->{
                                if(Objects.equals(stat.getCount(), max)) {
                                    if (statsDTO.getAmount().isEmpty()) {
                                        statsDTO.setAmount(stat.getCount() + " by " + stat.getPlayerName() + " ("+stat.getYear() +" )") ;
                                    } else {
                                        statsDTO.setAmount(statsDTO.getAmount() + " , " + stat.getPlayerName() + " ("+stat.getYear() +" )");
                                    }
                                }
                            }
                            case "Youngest Winner", "Oldest Winner" -> {
                                statsDTO.setAmount(stat.getPlayerName() + " age " + stat.getCount());
                            }
                        }

                    }
            );
        }
        return new ArrayList<>(statMap.values());
    }
}
