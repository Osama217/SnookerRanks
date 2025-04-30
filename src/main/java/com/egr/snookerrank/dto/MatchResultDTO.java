package com.egr.snookerrank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchResultDTO {
    private Integer eventKey;
    private String tournamnetName;
    private String date;
    private Integer year;
    private String winnerName;
    private Integer winnerKey;
    private String loserName;
    private Integer loserKey;
    private Integer winnerScore;
    private Integer loserScore;
    private Number prizeFund;
    private String score;

    public MatchResultDTO(Integer eventKey, Integer year, String winnerName, Integer winnerKey, String loserName, Integer loserKey, Integer winnerScore, Integer loserScore, Number prizeFund) {
        this.eventKey = eventKey;
        this.year = year;
        this.winnerName = winnerName;
        this.winnerKey = winnerKey;
        this.loserName = loserName;
        this.loserKey = loserKey;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
        this.prizeFund = prizeFund;
    }

    public MatchResultDTO(Integer eventKey, Integer year, String winnerName, Integer winnerKey, String loserName, Integer loserKey, Integer winnerScore, Integer loserScore, Number prizeFund, String score) {
        this.eventKey = eventKey;
        this.year = year;
        this.winnerName = winnerName;
        this.winnerKey = winnerKey;
        this.loserName = loserName;
        this.loserKey = loserKey;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
        this.prizeFund = prizeFund;
    }

    public MatchResultDTO(Integer eventKey, String tournamnetName, String date, Integer year, String winnerName, Integer winnerKey, String loserName, Integer loserKey, Integer winnerScore, Integer loserScore, Number prizeFund) {
        this.eventKey = eventKey;
        this.tournamnetName = tournamnetName;
        this.date = date;
        this.year = year;
        this.winnerName = winnerName;
        this.winnerKey = winnerKey;
        this.loserName = loserName;
        this.loserKey = loserKey;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
        this.prizeFund = prizeFund;
    }
}

