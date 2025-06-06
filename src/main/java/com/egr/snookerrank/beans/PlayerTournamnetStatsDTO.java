package com.egr.snookerrank.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PlayerTournamnetStatsDTO {
    private Integer playerKey;
    private String playerName;
    private String countryName;
    private String stats;
    private Number stats2;



    // Case 4 (example data): Tournament-specific statistics
    private Number wins;
    private Number ru; // Runner-ups
    private Number sf; // Semi-finals
    private Number qf; // Quarter-finals
    private Number l16; // Last 16
    private Number l32; // Last 32
    private Number l64; // Last 64
    private Number placings; // Total placements

    public PlayerTournamnetStatsDTO(Integer playerKey, String playerName, String countryName, String stats) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.countryName = countryName;
        this.stats = stats;
    }

    public PlayerTournamnetStatsDTO(Integer playerKey, String playerName, String countryName, String stats, Number stats2) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.countryName = countryName;
        this.stats = stats;
        this.stats2 = stats2;
    }

    public PlayerTournamnetStatsDTO(String countryName, Number wins, Number ru, Number sf, Number qf, Number l16, Number l32, Number l64, Number placings) {
        this.countryName = countryName;
        this.wins = wins;
        this.ru = ru;
        this.sf = sf;
        this.qf = qf;
        this.l16 = l16;
        this.l32 = l32;
        this.l64 = l64;
        this.placings = placings;
    }

    public PlayerTournamnetStatsDTO(Integer playerKey, String playerName, String countryName, Number wins, Number ru, Number sf, Number qf, Number l16, Number l32, Number l64, Number placings) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.countryName = countryName;
        this.wins = wins;
        this.ru = ru;
        this.sf = sf;
        this.qf = qf;
        this.l16 = l16;
        this.l32 = l32;
        this.l64 = l64;
        this.placings = placings;
    }
}
