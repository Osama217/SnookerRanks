package com.egr.snookerrank.controller;

import com.egr.snookerrank.beans.PeriodType;
import com.egr.snookerrank.beans.PlayerTournamnetStatsDTO;
import com.egr.snookerrank.beans.TournamnetStatsOption;
import com.egr.snookerrank.bl.SnookerStats;
import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.*;
import com.egr.snookerrank.model.Player;
import com.egr.snookerrank.service.PlayerService;
import com.egr.snookerrank.service.PlayerTournamnetStatsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.antlr.v4.runtime.misc.Pair;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Player APIs", description = "Endpoints for managing player data")
public class PlayerController {

    private final PlayerService playerService;
    private final SnookerStats snookerStats;
    private final PlayerTournamnetStatsServices playerTournamnetStatsServices;

    public PlayerController(PlayerService playerService, SnookerStats snookerStats,PlayerTournamnetStatsServices playerTournamnetStatsServices) {
        this.playerService = playerService;
        this.snookerStats  = snookerStats;
        this.playerTournamnetStatsServices = playerTournamnetStatsServices;
    }

    @Operation(
            summary = "Get top players",
            description = "Fetch top n number of players",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
            }
    )
    @GetMapping("/topPlayers")
    public RestApiResponse<List<TopPlayersDTO>> getTopPlayers(@RequestParam int count) {
        int limit = (count > 0) ? count : 10;  // Default to 10 if count is invalid
        List<TopPlayersDTO> topPlayers = playerService.getTopPlayers(limit);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", topPlayers);
    }
    @Operation(
            summary = "Order of Merit",
            description = "Get Order of Merit",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order of Merit"),
            }
    )
    @GetMapping("/orderOfMerit")
    public RestApiResponse<List<OrderOfMeritDTO>> getOrderOfMerit(
            @RequestParam(defaultValue = "12") int monthsBack,
            @RequestParam(defaultValue = "10") int topCount,
            @RequestParam(required = false) List<Integer> excludedPlayers
    ) {
        List<OrderOfMeritDTO> orderOfMerits = playerService.getOrderOfMerit(monthsBack, topCount, excludedPlayers);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", orderOfMerits);
    }
    @Operation(
            summary = "Stats Meta Data",
            description = "Return Stats Meta Data that is fetched frm databse",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stats Meta Data"),
            }
    )
    @GetMapping("/getStatsMetaData")
    public RestApiResponse<StatsDTO> getStatsMetaData() {
        StatsDTO statsDTO = playerService.getStats();
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", statsDTO);
    }

    @Operation(
            summary = "Stats",
            description = "Return Complete Stats based on conditions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stats Data"),
            }
    )
    @GetMapping("/getStats")
    public RestApiResponse<List<PlayerStats>> getPlayers(
            @RequestParam LocalDate dDateFrom,
            @RequestParam LocalDate dDateTo,
            @RequestParam Integer rankKey,
            @RequestParam(required = false) Integer eventKey,
            @RequestParam(required = false) TournamentDTO tournament,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer minMatches
            ) {
        List<PlayerStats> l1 = playerService.fetchPlayerStats(dDateFrom, dDateTo,eventKey,tournament,year,rankKey,minMatches);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", l1);

    }

    @Operation(
            summary = "search player",
            description = "Search any player based on name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Player found"),
            }
    )
    @GetMapping("/searchPlayer")
    public RestApiResponse<List<TopPlayersDTO>> searchPlayer(@RequestParam String name) {
        List<TopPlayersDTO> players=playerService.searchPlayer(name);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", players);
    }

    @Operation(
            summary = "get player details",
            description = "Get Player details, Best major results, Other wins and Earns based on player key ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Player found"),
            }
    )

    @GetMapping("/playerDetails")
    public  RestApiResponse<PlayerDetailsDTO> playerDetails(@RequestParam Integer key,
                                                                     @RequestParam(defaultValue = "false") Boolean isRanking){
        PlayerDetailsDTO playerDetails=  playerService.playerDetails(key, isRanking);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", playerDetails);
    }


    @Operation(
            summary = "Annual Win Loss",
            description = "Get Annual Win Loss based on player key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Player found"),
            }
    )
    @GetMapping("/annualWinLoss")
    public  RestApiResponse<PlayerAdditionalDetailsDTO> annualWinLoss(@RequestParam Integer key){
        PlayerAdditionalDetailsDTO playerAddDetails=  playerService.annualWinLoss(key);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", playerAddDetails);
    }


    @Operation(
            summary = "Tournament Stats based on player",
            description = "Get Tournament Stats based on player key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Player found"),
            }
    )
    @GetMapping("/tournamentStats")
    public RestApiResponse<TournamentStatsDTO> tournamentStats(@RequestParam TournamnetStatsOption tournamnetStatsOption,
                                                   @RequestParam Integer playerKey
                                                   )
    {
        TournamentStatsDTO tournamentStats = playerService.getTournamentStats(tournamnetStatsOption,playerKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", tournamentStats);

    }
    @Operation(
            summary = "get tournamnetDetails",
            description = "Get tournamnet details ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TOurnamnet Details"),
            }
    )
    @GetMapping("/tournamnetDetails")
    public  RestApiResponse<List<MatchResultDTO>> tournamentDetails(@RequestParam Integer tournamentKey){
       List<MatchResultDTO> result = playerService.getTOurnamentDetals(tournamentKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", result);

    }

    @Operation(
            summary = "get event results",
            description = "get event results ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event Details"),
            }
    )
    @GetMapping("/eventResults")
    public RestApiResponse<EventResultsDTO> eventResults(@RequestParam Integer eventKey){
        EventResultsDTO eventResults = playerService.getEventResults(eventKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventResults);
    }

    @Operation(
            summary = "Search all events",
            description = "get events ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event Details"),
            }
    )
    @GetMapping("/eventList")
    public RestApiResponse<List<EventListDTO>> eventList(@RequestParam Integer year){
        List<EventListDTO> eventList = playerService.getEventList(year);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventList);

    }

    @Operation(
            summary = "Event prize funds",
            description = "get events prize funds ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event Details"),
            }
    )
    @GetMapping("/eventPrizeFund")
    public RestApiResponse<EventResultsDTO> eventPrizeFund(@RequestParam Integer eventKey){
        EventResultsDTO eventREsult = playerService.getEventPrizeFund(eventKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventREsult);
    }
    @Operation(
            summary = "Tournamnet Stats",
            description = "Tournamnet Stats ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournamnet Stats"),
            }
    )
    @GetMapping("/tournamentStatsSummary")
    public RestApiResponse<List<TournamnetStatsSummaryDTO>> tournamentStatsSummary(@RequestParam Integer tournamentKey){
        List<TournamnetStatsSummaryDTO> tournamentstats =  playerService.tournamentStatsSummary(tournamentKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", tournamentstats);
    }


    @Operation(
            summary = "Head2HeadList Provides details of H2H against the requested player",
            description = "Refer to player stats H2H",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Head2HeadList"),
            }
    )
    @GetMapping("/Head2HeadList")
    public RestApiResponse<H2HListDTO> head2HeadList(@RequestParam Integer playerId){
        H2HListDTO H2Hlist=  playerService.fetchHead2HeadList(playerId);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", H2Hlist);

    }


    @Operation(
            summary = "Head2HeadList Provides name of all players",
            description = "Refer to perdictor tools page",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Head2HeadList"),
            }
    )
    @GetMapping("/Head2HeadPlayersList")
    public RestApiResponse<List<PlayerDTO>> head2HeadPlayers()
    {
       List<PlayerDTO> players = playerService.fetchHead2HeadPlayersList();
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", players);
    }

    @Operation(
            summary = "Head2HeadchancesToWin Provides chances to win 2 players",
            description = "Refer to perdictor tools page",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Head2HeadList"),
            }
    )
    @GetMapping("/Head2HeadchancesToWin")
    public RestApiResponse<H2HChancesToWinDTO> chancesToWin(@RequestParam Double player1Fdi,@RequestParam Double player2Fdi, @RequestParam Integer firstTo){
        H2HChancesToWinDTO h2HChancesToWinDTO = new H2HChancesToWinDTO();
        Double nChancePlayer1 = snookerStats.chanceToWin(player1Fdi,player2Fdi,"L",firstTo);
       Double nChancePlayer2 = 1-nChancePlayer1;
        Pair<Double,Double> chanceForBothPlayers = new Pair<>(nChancePlayer1*100,nChancePlayer2*100);
        List<CorrectScoreDTO> correctScoreDTOS=snookerStats.calculateCorrectScores(firstTo,player1Fdi,player2Fdi);
        h2HChancesToWinDTO.setCorrectScoreDTOS(correctScoreDTOS);
        h2HChancesToWinDTO.setChancesToWin(chanceForBothPlayers);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", h2HChancesToWinDTO);

    }

    @Operation(
            summary = "Head2HeadchancesToWinmatchResult Provides chances to win 2 players",
            description = "Refer to perdictor tools page",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Head2HeadList"),
            }
    )
    @GetMapping("/Head2HeadchancesToWinmatchResult")
    public RestApiResponse<Head2HeadchancesToWinmatchResultDTO> getMatchResults(@RequestParam int player1Key, @RequestParam int player2Key) {
        Head2HeadchancesToWinmatchResultDTO dto = playerService.getMatchResults(player1Key, player2Key);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", dto);

    }


    @Operation(
            summary = "TalenetPortalPlayerReport",
            description = "Refer to TalenetPortal PlayerReport page",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TalenetPortal"),
            }
    )
    @GetMapping("/TalenetPortalPlayerReport")
    public RestApiResponse< Map<String, List<RankDetails>>> talenetPortal(@RequestParam Integer playerKey, @RequestParam PeriodType period) {
        Map<String, List<RankDetails>> resp = playerService.getTalentPortal(playerKey,period);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", resp);

    }

    @Operation(
            summary = "TalenetPortalPlayerReport",
            description = "Refer to TalenetPortal Head to head page. Date formats are yyyy-MM-dd",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TalenetPortal"),
            }
    )
    @GetMapping("/TalenetPortalH2H")
    public RestApiResponse<Map<Integer, List<RankDetails>>> talenetPortalHead2Head(@RequestParam Integer player1Key,@RequestParam Integer player2Key,
                                                                                                                         @RequestParam String dateFrom, @RequestParam String dateTo) {
        if (!dateFrom.matches("\\d{4}-\\d{2}-\\d{2}") || !dateTo.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd");
        }
        Map<Integer, List<RankDetails>> response = new LinkedHashMap<>();

        playerService.getTalentPortalHead2Head(player1Key,dateFrom,dateTo,response);
        playerService.getTalentPortalHead2Head(player2Key,dateFrom,dateTo,response);

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", response);

    }
    @Operation(
            summary = "TalenetPortaFDIComparison",
            description = "Refer to TalenetPortal FDI Comparison page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TalenetPortal"),
            }
    )
    @GetMapping("/TalenetPortaFDIComparison")
    public RestApiResponse<List<FDIComparisonDTO>> talenetPortaFDIComparison() {

       List<FDIComparisonDTO> list = playerService.talenetPortaFDIComparison();

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", list);
    }

    @Operation(
            summary = "RankingsMetaData",
            description = "Refer to Rankings page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "RankingsMetaData"),
            }
    )
    @GetMapping("/RankingsMetaData")
    public RestApiResponse<RankingMetaData> rankingsMetaData() {

        RankingMetaData data = playerService.rankingsMetaData();

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", data);
    }

    @Operation(
            summary = "Rankings",
            description = "Refer to Rankings page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rankings"),
            }
    )
    @GetMapping("/Rankings")
    public RestApiResponse<RankingDTO> rankings(@RequestParam Integer rankKey,@RequestParam String dateFrom, @RequestParam String dateTo, @RequestParam(required = false) String country, @RequestParam Integer maxAge, @RequestParam(required = false) boolean isWomen) {

        RankingDTO rankingDTO = playerService.rankings(rankKey,dateFrom,dateTo,country,maxAge,isWomen);

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", rankingDTO);
    }

    @Operation(
            summary = "PlayerStatsLast12Month",
            description = "Refer to Player Details page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PlayerStatsLast12Month"),
            }
    )
    @GetMapping("/PlayerStatsLast12Month")
    public RestApiResponse<Map<Integer, List<RankDetails>>> PlayerStatsLast12Month(@RequestParam Integer playerKey) {
        Map<Integer, List<RankDetails>> response =new LinkedHashMap<>();
        LocalDate dDateTo = LocalDate.now(); // Today's date
        LocalDate dDateFrom = dDateTo.minusYears(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        playerService.getTalentPortalHead2Head(playerKey, formatter.format(dDateFrom),formatter.format(dDateTo),response);


        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", response);
    }

    @Operation(
            summary = "PlayerDetailStats",
            description = "Refer to Player Details page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PlayerDetailStats"),
            }
    )
    @GetMapping("/PlayerDetailStats")
    public RestApiResponse<List<PlayerStats>> PlayerDetailStats(@RequestParam Integer playerKey,@RequestParam(required = false)Integer rankKey) {
        if(null ==rankKey)
            rankKey= 101;
        List<PlayerStats> stats= playerService.playerDetailStats(playerKey,rankKey);

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", stats);
    }

    @Operation(
            summary = "playerTournamamnetStatsMetaData",
            description = "Refer to TournamentPlayerStats.aspx page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PlayerDetailStats"),
            }
    )
    @GetMapping("/playerTournamamnetStatsMetaData")
    public RestApiResponse< Map<Integer, String>> playerTournamamnetStatsMetaData() {

        Map<Integer, String> metaData =  playerTournamnetStatsServices.getRankings();

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", metaData);
    }

    @Operation(
            summary = "playerTournamamnetStats",
            description = "Refer to TournamentPlayerStats.aspx page.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PlayerDetailStats"),
            }
    )
    @GetMapping("/playerTournamamnetStats")
    public RestApiResponse<List<PlayerTournamnetStatsDTO>> playerTournamamnetStats(@RequestParam Integer tournamnetKey,@RequestParam Integer statsKey, @RequestParam String dateFrom, @RequestParam String dateTo) {

        List<PlayerTournamnetStatsDTO> statsDTOS =  playerTournamnetStatsServices.getStats(tournamnetKey,statsKey,dateFrom,dateTo);

        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", statsDTOS);
    }

    }