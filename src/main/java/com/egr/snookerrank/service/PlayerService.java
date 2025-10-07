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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        RankText rankText;
        if (rankKey.equals(50) || rankKey.equals(51) || rankKey.equals(52) || rankKey.equals(53) || rankKey.equals(54)) {
           rankText = new RankText();
            result = playerStatsRepository.findPlayersWithFilters(dDateFrom, dDateTo, eventKey, tournament, year, rankKey);

        } else {
             rankText = rankTextRepository.findByRankTextKey(rankKey);
            if (rankText != null && rankText.isMatchStat()) {
                result = playerStatsRepository.findPlayersWithStatsRankingFilters(rankText.getStatType(), rankText.getField1(), rankText.getField2(),
                        tournament, year, eventKey, dDateFrom, dDateTo, minMatches, rankText.isOrderAsc(), 500);
            }
        }
        List<PlayerStats> ranksDTOList = result.stream().map(row -> {
            if (row.length > 4) {
                Number n1 =  null;
                if(row.length > 5 && row[5] != null ) {
                   if(rankText.getStatType().equals("P")) {
                        n1 = new BigDecimal(((Number) row[5]).doubleValue() *100).setScale(2, RoundingMode.HALF_UP);
                    }else{
                       n1 = new BigDecimal(((Number) row[5]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
                   }
                }

                return new PlayerStats(
                        (Integer) row[0],
                        (String) row[1],
                        (String) row[2],
                        n1,
                        (Number) row[3],
                        (Number) row[4],
                         ((Number) row[3]).intValue() + "/" + ((Number) row[4]).intValue()
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
            //combine same tournament years
            Map<Integer, TournamentEventDTO> merged = new LinkedHashMap<>();
            for (TournamentEventDTO e : tournamentEvents) {
                if (e == null) continue;

                TournamentEventDTO existing = merged.get(e.getTournamentKey());

                if (existing == null) {
                    // First time we see this tournament → create new DTO with years list
                    e.setYears(new ArrayList<>(List.of(e.getEventDate())));
                    merged.put(e.getTournamentKey(), e);
                } else {
                    // Already exists → just add the year if not already present
                    if (existing.getYears() == null) {
                        existing.setYears(new ArrayList<>());
                    }
                    if (e.getEventDate() != null && !existing.getYears().contains(e.getEventDate())) {
                        existing.getYears().add(e.getEventDate());
                    }
                }
            }

            // Final combined list
            List<TournamentEventDTO> combinedList = new ArrayList<>(merged.values());
            // sort the years in the list
            for (TournamentEventDTO dto : combinedList) {
                dto.getYears().sort(Integer::compareTo);
            }
            playerDetails.setOtherTournamentEvents(combinedList);

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
                            match.setScore(match.getWinnerScore() + " V " + match.getLoserScore());
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
                    .filter(prize ->null != prize.getPrizeMoney() && CommonUtilities.isGreaterThan(prize.getPrizeMoney(),0) && !prize.getMatchResults().isEmpty())
                    .collect(Collectors.toList());
        }
        return new EventResultsDTO(tournamentCompleteName, prizeFundsDTOList);


    }

    public List<EventListDTO> getEventList(Integer year) {
        List<EventListDTO> eventList = playerRepository.getEventList(year);
        eventList = eventList.stream().filter(event->CommonUtilities.isGreaterThan(event.getPrizeFund(),0)).toList();
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
        Map<String, Integer> highest = new LinkedHashMap<>();
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
            stats.addAll(playerRepository.getMost147sInTournamnet(tournamentKey));

            stats.forEach(stat -> {
                highest.putIfAbsent(stat.getRoundLabel(), 0);
                Integer max = highest.get(stat.getRoundLabel());
                if (stat.getCount() > max) {
                    highest.put(stat.getRoundLabel(), stat.getCount());
                }
            });


            stats.forEach(
                    stat -> {
                        statMap.putIfAbsent(stat.getRoundLabel(), new TournamnetStatsSummaryDTO(stat.getRoundLabel(), ""));
                        TournamnetStatsSummaryDTO statsDTO = statMap.get(stat.getRoundLabel());
                        Integer max = highest.get(stat.getRoundLabel());
                        switch (stat.getRoundLabel()) {
                            case "Most Wins", "Most Finals", "Most Quarter Finals", "Most Semi Finals"
                            , "Most Appearances", "Most Century Breaks in a Match"  -> {
                                if (Objects.equals(stat.getCount(), max)) {
                                    if (statsDTO.getAmount().isEmpty()) {
                                        statsDTO.setAmount(stat.getCount() + " by " + stat.getPlayerName());
                                    } else {
                                        statsDTO.setAmount(statsDTO.getAmount() + " , " + stat.getPlayerName());
                                    }
                                }
                            }
                            case "Most Century Breaks in a Tournament" -> {
                                if (Objects.equals(stat.getCount(), max)) {
                                    if (statsDTO.getAmount().isEmpty()) {
                                        statsDTO.setAmount(stat.getCount() + " by " + stat.getPlayerName() + " (" + stat.getYear() + ")");
                                    } else {
                                        statsDTO.setAmount(statsDTO.getAmount() + " , " + stat.getPlayerName() + " (" + stat.getYear() + ")");
                                    }
                                }
                            }
                            case "Youngest Winner", "Oldest Winner" -> {
                                statsDTO.setAmount(stat.getPlayerName() + " age " + stat.getCount());
                            }
                            case "Most 147s"  -> {
                                if (Objects.equals(stat.getCount(), max)) {
                                    if (statsDTO.getAmount().isEmpty()) {
                                        if(stat.getCount()>0)
                                            statsDTO.setAmount(stat.getCount() + " by " + stat.getPlayerName());
                                        else
                                            statsDTO.setAmount("-");
                                    } else {
                                        statsDTO.setAmount(statsDTO.getAmount() + " , " + stat.getPlayerName());
                                    }
                                }
                            }

                        }

                    }
            );
        }
        return new ArrayList<>(statMap.values());
    }

    public H2HListDTO fetchHead2HeadList(Integer playerId) {
        H2HListDTO h2HListDTO = new H2HListDTO();
        Player player = playerRepository.findByPlayerKey(playerId);
        if (null == player) {
            throw new RuntimeException("No Player found");
        }
        List<PlayerH2HStatsDTO> H2hList = playerRepository.getCompleteH2HList(playerId);
        h2HListDTO.setPlayerH2HStats(H2hList);
        h2HListDTO.getPlayerH2HStats().forEach(h2H ->
                h2H.setPcnt(CommonUtilities.roundToTwoDecimals(h2H.getPcnt())));
        h2HListDTO.setName(player.getPlayerName());
        return h2HListDTO;

    }

    public List<PlayerDTO> fetchHead2HeadPlayersList() {
        List<PlayerDTO> playerDTOList = new ArrayList<>();
        List<Player> players = playerRepository.findAllH2HPlayers();
        for (Player p : players) {
            PlayerDTO playerDTO = new PlayerDTO(p.getPlayerKey(), p.getPlayerName(), CommonUtilities.roundToTwoDecimals(p.getFdi()), p.getCountryName(), p.getAge());
            playerDTOList.add(playerDTO);

        }
        return playerDTOList;
    }

    public Head2HeadchancesToWinmatchResultDTO getMatchResults(int player1Key, int player2Key) {
        // Retrieve raw match data using JPA native query
        Head2HeadchancesToWinmatchResultDTO head2HeadchancesToWinmatchResultDTO = new Head2HeadchancesToWinmatchResultDTO();
        List<Object[]> rows = playerRepository.findMatchStats(player1Key, player2Key);

        // Initialize counts array
        int[][] arrCounts = new int[5][3]; // [5 categories x 3 (wins, draws, losses)]
        for (Object[] row : rows) {
            String category = (String) row[4]; // e.event_category
            int winnerScore = (Integer) row[10]; // m.winner_score
            int loserScore = (Integer) row[11]; // m.loser_score

            // Map category to index
            int categoryIndex = getCategory(category);
            // Determine who won and who lost
            Integer winnerKey = (Integer) row[6]; // m.winner_key
            Integer loserKey = (Integer) row[7];  // m.loser_key

            if (winnerKey.equals(player1Key)) {
                if (winnerScore != loserScore && winnerScore != 0) {
                    arrCounts[categoryIndex][0]++; // Player 1 wins
                    arrCounts[4][0]++; // Totals
                } else {
                    arrCounts[categoryIndex][1]++; // Draw
                    arrCounts[4][1]++; // Totals
                }
            } else {
                if (winnerScore != loserScore && winnerScore != 0) {
                    arrCounts[categoryIndex][2]++; // Player 2 wins
                    arrCounts[4][2]++; // Totals
                } else {
                    arrCounts[categoryIndex][1]++; // Draw
                    arrCounts[4][1]++; // Totals
                }
            }
        }
        List<MatchResultCategoryWiseDTO> stats = new ArrayList<>();
        stats.add(new MatchResultCategoryWiseDTO("World Championship", arrCounts[0][0], arrCounts[0][1], arrCounts[0][2]));
        stats.add(new MatchResultCategoryWiseDTO("Ranked Majors", arrCounts[1][0], arrCounts[1][1], arrCounts[1][2]));
        stats.add(new MatchResultCategoryWiseDTO("Other Ranking", arrCounts[2][0], arrCounts[2][1], arrCounts[2][2]));
        stats.add(new MatchResultCategoryWiseDTO("Non Ranking", arrCounts[3][0], arrCounts[3][1], arrCounts[3][2]));
        stats.add(new MatchResultCategoryWiseDTO("Totals", arrCounts[4][0], arrCounts[4][1], arrCounts[4][2]));
        head2HeadchancesToWinmatchResultDTO.setRecentMeetings(stats);
        List<MatchResultsWithOrder> statswithOrder = playerRepository.findMatchStatsWithOrder(player1Key, player2Key);
        statswithOrder.forEach(stat -> {
            stat.setScore(stat.getWinnerScore() + " V " + stat.getLoserScore());
            if (stat.getLoserKey().equals(player1Key)) {
                stat.setResult("Lost");
            } else {
                stat.setResult("Won");
            }
        });

        head2HeadchancesToWinmatchResultDTO.setStatswithOrder(statswithOrder);
        return head2HeadchancesToWinmatchResultDTO;
    }

    private int getCategory(String eventCategory) {
        if ("WC".equals(eventCategory)) return 0;
        if ("MJ".equals(eventCategory)) return 1;
        if ("U".equals(eventCategory) || "0".equals(eventCategory)) return 2;
        return 3; // default for "Other Ranking" or "Non Ranking"
    }

    public Map<String, List<RankDetails>> getTalentPortal(Integer playerKey, PeriodType period) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();  // Start from today's date
        List<Map<String, Integer>> data = null;
        Map<String, List<RankDetails>> response = new HashMap<>();
        List<RankFields> ranks = playerRepository.getTalentPortalRanks();
        int months = period.getNumberOfMonths();
        for (int i = 0; i < 6; i++) {
            List<RankDetails> finalResponseBasedOnDate = new ArrayList<>();
            String dateEnd = sdf.format(calendar.getTime());
            calendar.add(Calendar.MONTH, -months);
            String dateStart = sdf.format(calendar.getTime());
            data = playerRepository.getTalentDEtailsByDate(playerKey, dateStart, dateEnd);
            double nCompare1;
            String result = "";

            for (RankFields rank : ranks) {
                switch (rank.getStatType()) {
                    case "D":
                        if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                            nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                            result = String.format("%.2f", Math.round(nCompare1 * 100.0) / 100.0);
                        }
                        break;

                    case "A":
                        if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                            nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                            result = String.format("%.2f", Math.round(nCompare1 * 3.0 * 100.0) / 100.0);
                        }
                        break;

                    case "P":
                        if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                            nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                            result = String.format("%.2f", ((Math.round(nCompare1 * 10000.0) / 10000.0)) * 100) + "%";
                        }
                        break;

                    case "X":
                        if (null != data.getFirst().get(rank.getField1()) && data.getFirst().get(rank.getField1()) != -1) {
                            result = String.valueOf(data.getFirst().get(rank.getField1()));

                        }
                        break;
                    
                    case "%deciding_frames_won":
                        result = String.format("%.2f", data.getFirst().get("deciders_win_percentage")) + "%";
                        break;

                    case "%matches_won":
                        result = String.format("%.2f", data.getFirst().get("match_win_percentage")) + "%";
                        break;
                    
                    case "50_in_deciding":
                        result = String.format("%.2f", data.getFirst().get("breaks_50_plus_deciders_percentage")) + "%";
                        break;

                    case "70_in_deciding":
                        result = String.format("%.2f", data.getFirst().get("breaks_70_plus_deciders_percentage")) + "%";
                        break;
                    
                    case "century_in_deciding":
                        result = String.format("%.2f", data.getFirst().get("breaks_100_plus_deciders_percentage")) + "%";
                        break;

                    case "av_deficit_frames_won":
                        result = String.format("%.2f", data.getFirst().get("avg_points_deficit_won_frames"));
                        break;
                    
                    case "av_deficit_frames_lost":
                        result = String.format("%.2f", data.getFirst().get("avg_points_deficit_lost_frames"));
                        break;

                    case "%opening_frames_won":
                        result = String.format("%.2f", data.getFirst().get("opening_frame_win_percentage")) + "%";
                        break;

                    case "%opening_two_frames_won":
                        result = String.format("%.2f", data.getFirst().get("opening_2_frames_win_percentage")) + "%";
                        break;

                    default:
                        result = String.valueOf(data.getFirst().get(rank.getField1()));
                        break;

                    }
                finalResponseBasedOnDate.add(new RankDetails(rank.getRankKey(), rank.getRankName(),result));


            }
            response.putIfAbsent(dateStart + " - " + dateEnd, finalResponseBasedOnDate);

        }


        return response;

    }

    public void getTalentPortalHead2Head(Integer playerKey, String dateFrom, String dateTo, Map<Integer, List<RankDetails>> response) {
        List<Map<String, Integer>> data = null;
        List<RankFields> ranks = playerRepository.getTalentPortalRanks();
        List<RankDetails> finalResponseBasedOnDate = new ArrayList<>();

        data = playerRepository.getTalentDEtailsByDate(playerKey, dateFrom, dateTo);
        double nCompare1;
        String result = "";

        for (RankFields rank : ranks) {
            switch (rank.getStatType()) {
                case "D":
                    if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                        nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                        result = String.format("%.2f", Math.round(nCompare1 * 100.0) / 100.0);
                    }
                    break;

                case "A":
                    if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                        nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                        result = String.format("%.2f", Math.round(nCompare1 * 3.0 * 100.0) / 100.0);
                    }
                    break;

                case "P":
                    if (null != data.getFirst().get(rank.getField2()) && data.getFirst().get(rank.getField2()) > 0) {
                        nCompare1 = data.getFirst().get(rank.getField1()) / (double) data.getFirst().get(rank.getField2());
                        result = String.format("%.2f", ((Math.round(nCompare1 * 10000.0) / 10000.0)) * 100) + "%";
                    }
                    break;

                case "X":
                    if (null != data.getFirst().get(rank.getField1()) && data.getFirst().get(rank.getField1()) != -1) {
                        result = null != data.getFirst().get(rank.getField1()) ?String.valueOf(data.getFirst().get(rank.getField1())) : "";

                    }
                    break;

                case "%deciding_frames_won":
                    result = String.format("%.2f", data.getFirst().get("deciders_win_percentage")) + "%";
                    break;

                case "%matches_won":
                    result = String.format("%.2f", data.getFirst().get("match_win_percentage")) + "%";

                    break;
                
                case "50_in_deciding":
                    result = String.format("%.2f", data.getFirst().get("breaks_50_plus_deciders_percentage")) + "%";
                    break;

                case "70_in_deciding":
                    result = String.format("%.2f", data.getFirst().get("breaks_70_plus_deciders_percentage")) + "%";
                    break;
                
                case "century_in_deciding":
                    result = String.format("%.2f", data.getFirst().get("breaks_100_plus_deciders_percentage")) + "%";
                    break;

                case "av_deficit_frames_won":
                    result = String.format("%.2f", data.getFirst().get("avg_points_deficit_won_frames"));
                    break;
                
                case "av_deficit_frames_lost":
                    result = String.format("%.2f", data.getFirst().get("avg_points_deficit_lost_frames"));
                    break;

                case "%opening_frames_won":
                    result = String.format("%.2f", data.getFirst().get("opening_frame_win_percentage")) + "%";
                    break;

                case "%opening_two_frames_won":
                    result = String.format("%.2f", data.getFirst().get("opening_2_frames_win_percentage")) + "%";
                    break;

                default:
                    result = null != data.getFirst().get(rank.getField1())   ? String.valueOf(data.getFirst().get(rank.getField1())) :"";
                    break;
            }

            finalResponseBasedOnDate.add(new RankDetails(rank.getRankKey(),rank.getRankName(),result));

        }
        response.putIfAbsent(playerKey, finalResponseBasedOnDate);

    }

    public List<FDIComparisonDTO> talenetPortaFDIComparison() {
      return  playerRepository.gettalenetPortaFDIComparison();
    }

    public RankingMetaData rankingsMetaData() {
        RankingMetaData rankingMetaData = new RankingMetaData();
        List<Object[]> list =  playerRepository.getRanksForRanking();

        for(Object[] obj : list){
            rankingMetaData.getRanks().add(Pair.of(
                    (Integer) obj[0],
                    (String) obj[1]
            ));
        }
        List<String> countryNames = playerRepository.getCountryNames();
        rankingMetaData.setCountryNames(countryNames);
        return rankingMetaData;

    }


    public RankingDTO rankings(Integer rankKey, String dateFrom, String dateTo, String country, Integer maxAge, boolean isWomen) {
        RankingDTO rankingDTO = new RankingDTO();
        List<OrderOfMeritDTO> orderOfMeritDTOS = null;
        List<PlayerDTO> playerDTOS = new ArrayList<>();
        if(rankKey.equals(1)) {
            LocalDate date = LocalDate.parse(dateTo, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            int effectiveYear = (date.getMonthValue() == 1 && date.getDayOfMonth() < 7) ? date.getYear() - 1 : date.getYear();

            List<Object[]> result = playerRepository.findOrderOfMerit(dateFrom, dateTo, 500, effectiveYear, null);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            orderOfMeritDTOS = result.stream().map(row -> new OrderOfMeritDTO(
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
        } else {
          playerDTOS =  playerRepository.findPlayers(rankKey,country,isWomen?1:null,maxAge);
          playerDTOS.forEach(player-> player.setFdi(CommonUtilities.safeInt(player.getFdi())));
        }

        rankingDTO.setOrderOfMerit(orderOfMeritDTOS);
        rankingDTO.setPlayers(playerDTOS);
    return rankingDTO;
    }

    public  List<PlayerStats> playerDetailStats(Integer playerKey,Integer rankKey) {
        RankText rankText = rankTextRepository.findByRankTextKey(rankKey);
        List<Object[]> result;
        if (rankText != null && rankText.isMatchStat()) {
            LocalDate dDateTo = LocalDate.now(); // Today's date
            LocalDate dDateFrom = dDateTo.minusYears(1); // One year ago
            result = playerStatsRepository.findPlayersWithStatsRankingFilters(rankText.getStatType(), rankText.getField1(), rankText.getField2(),
                    null, null, null, dDateFrom, dDateTo, 1, rankText.isOrderAsc(), 32);
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
        return null;
    }

    public MatchPlayerStatsDTO getMatchPlayerStats(Integer matchKey, Integer winnerKey, Integer losserKey) {
        MatchPlayerStatsDTO dto = null;
        List<Integer>  desiredOrder = Arrays.asList(
                201, 152, 153, 151, 102, 108, 101, 502, 132, 143
        );
        List<Map<String, Object>> player1List =    playerRepository.getMatchPlayerStats(matchKey,winnerKey);
        List<Map<String, Object>> player2List =    playerRepository.getMatchPlayerStats(matchKey,losserKey);
     List<RankFields> ranks = playerRepository.getTalentPortalRanks();
        Map<Integer, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < desiredOrder.size(); i++) {
            orderMap.put(desiredOrder.get(i), i);
        }

// Filter and sort
        List<RankFields> sortedRanks = ranks.stream()
                .filter(r -> orderMap.containsKey(r.getRankKey()))  // keep only desired keys
                .sorted(Comparator.comparingInt(r -> orderMap.get(r.getRankKey())))
                .collect(Collectors.toList());

        Map <String,String> player1result = getResult(player1List,sortedRanks);
        Map <String,String> player2result = getResult(player2List,sortedRanks);
        dto = new MatchPlayerStatsDTO(player1result,player2result);
        return dto;

    }

    private Map <String,String> getResult(List<Map<String, Object>> data, List<RankFields> ranks) {
    Map <String,String> map= new LinkedHashMap<>();
        for (RankFields rank : ranks) {
            String result = "";
            Double nCompare1;
            switch (rank.getStatType()) {
                case "D":
                    if (CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) !=0 && CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) != -1){
                        nCompare1 = getDouble(data.getFirst().get(rank.getField1())) / getDouble(data.getFirst().get(rank.getField2()));
                        result = String.format("%.2f", Math.round(nCompare1 * 100.0) / 100.0);
                    }
                    break;

                case "A":
                    if (CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) !=0 && CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) != -1){
                        nCompare1 = getDouble(data.getFirst().get(rank.getField1())) / getDouble(data.getFirst().get(rank.getField2()));
                        result = String.format("%.2f", Math.round(nCompare1 * 3.0 * 100.0) / 100.0);
                    }
                    break;

                case "P":
                    if (CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) !=0 && CommonUtilities.stat2Value(data.getFirst(),rank.getField2()) != -1){
                        nCompare1 = getDouble(data.getFirst().get(rank.getField1())) / getDouble(data.getFirst().get(rank.getField2()));
                        result = String.format("%.2f", ((Math.round(nCompare1 * 10000.0) / 10000.0)) * 100) + "%";
                    }
                    break;

                case "X":
                    if (CommonUtilities.stat2Value(data.getFirst(),rank.getField1()) !=0 && CommonUtilities.stat2Value(data.getFirst(),rank.getField1()) != -1){
                        result = String.valueOf(data.getFirst().get(rank.getField1()));

                    }
                    break;
                default:
                    result = String.valueOf(data.getFirst().get(rank.getField1()));
                    break;

            }
            map.putIfAbsent(rank.getRankName(),result);
        }
        return map;
    }



    public Double getDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue(); // handles other numeric types too
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<MatchResultDTO> getLatestResult() {
         return playerRepository.getLatestResult();
    }
}





