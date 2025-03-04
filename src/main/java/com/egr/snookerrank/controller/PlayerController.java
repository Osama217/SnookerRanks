package com.egr.snookerrank.controller;

import com.egr.snookerrank.dto.*;
import com.egr.snookerrank.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/top")
    public ApiResponse<List<TopPlayersDTO>> getTopPlayers(@RequestParam int count) {
        int limit = (count > 0) ? count : 10;  // Default to 10 if count is invalid
        List<TopPlayersDTO> topPlayers = playerService.getTopPlayers(limit);
        return new ApiResponse<>("SUCCESS", "Data fetched successfully", topPlayers);
    }

    @GetMapping("/order-of-merit")
    public ApiResponse<List<OrderOfMeritDTO>> getOrderOfMerit(
            @RequestParam(defaultValue = "12") int monthsBack,
            @RequestParam(defaultValue = "10") int topCount,
            @RequestParam(required = false) List<Integer> excludedPlayers
    ) {
        List<OrderOfMeritDTO> orderOfMerits = playerService.getOrderOfMerit(monthsBack, topCount, excludedPlayers);
        return new ApiResponse<>("SUCCESS", "Data fetched successfully", orderOfMerits);
    }

    @GetMapping("/getStatsMetaData")
    public ApiResponse<StatsDTO> getStatsMetaData() {
        StatsDTO statsDTO = playerService.getStats();
        return new ApiResponse<>("SUCCESS", "Data fetched successfully", statsDTO);
    }
}