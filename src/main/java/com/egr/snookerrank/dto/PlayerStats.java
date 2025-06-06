package com.egr.snookerrank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerStats {
    private Integer playerKey;
    private String playerName;
    private String countryName;
    private Number stats;
    private Number seventyBreaks ;
    private Number framesPlayed ;
    private String details;


    public PlayerStats( Integer playerKey, String playerName,String countryName,Number stats) {
        this.stats = stats;
        this.countryName = countryName;
        this.playerName = playerName;
        this.playerKey = playerKey;
    }

    public PlayerStats(Integer playerKey, String playerName, String countryName, Number stats, Number seventyBreaks, Number framesPlayed) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.countryName = countryName;
        this.stats = stats;
        this.seventyBreaks = seventyBreaks;
        this.framesPlayed = framesPlayed;
    }
}
