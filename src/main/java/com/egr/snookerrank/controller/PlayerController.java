package com.egr.snookerrank.controller;

import com.egr.snookerrank.beans.TournamnetStatsOption;
import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.dto.response.*;
import com.egr.snookerrank.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Player APIs", description = "Endpoints for managing player data")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
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
            summary = "Tournament Stats",
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

    @GetMapping("/tournamnetDetails")
    public  RestApiResponse<List<MatchResultDTO>> tournamentDetails(@RequestParam Integer tournamentKey){
       List<MatchResultDTO> result = playerService.getTOurnamentDetals(tournamentKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", result);

    }
    @GetMapping("/eventResults")
    public RestApiResponse<EventResultsDTO> eventResults(@RequestParam Integer eventKey){
        EventResultsDTO eventResults = playerService.getEventResults(eventKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventResults);
    }

    @GetMapping("/eventList")
    public RestApiResponse<List<EventListDTO>> eventList(@RequestParam Integer year){
        List<EventListDTO> eventList = playerService.getEventList(year);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventList);

    }

    @GetMapping("/eventPrizeFund")
    public RestApiResponse<EventResultsDTO> eventPrizeFund(@RequestParam Integer eventKey){
        EventResultsDTO eventREsult = playerService.getEventPrizeFund(eventKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", eventREsult);
    }
    @GetMapping("/tournamentStatsSummary")
    public RestApiResponse<List<TournamnetStatsSummaryDTO>> tournamentStatsSummary(@RequestParam Integer tournamentKey){
        List<TournamnetStatsSummaryDTO> tournamentstats =  playerService.tournamentStatsSummary(tournamentKey);
        return new RestApiResponse<>("SUCCESS", "Data fetched successfully", tournamentstats);
    }

}