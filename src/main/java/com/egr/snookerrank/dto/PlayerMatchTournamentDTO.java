package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerMatchTournamentDTO {
    private Integer tournamentKey;
    private Integer eventKey;
    private String tournamentName;
    private Integer tournamentNo;
    private Date matchDate;
    private String eventCategory;
    private String roundName;
    private Integer winnerKey;
    private Integer loserKey;
    private String winnerName;
    private String loserName;
    private Integer winnerScore;
    private Integer loserScore;
    private Boolean isBye;
    private String result;
    private String score;

    public PlayerMatchTournamentDTO(Integer tournamentKey, Integer eventKey, String tournamentName, Integer tournamentNo, Date matchDate, String eventCategory, String roundName, Integer winnerKey, Integer loserKey, String winnerName, String loserName, Integer winnerScore, Integer loserScore, Boolean isBye) {
        this.tournamentKey = tournamentKey;
        this.eventKey = eventKey;
        this.tournamentName = tournamentName;
        this.tournamentNo = tournamentNo;
        this.matchDate = matchDate;
        this.eventCategory = eventCategory;
        this.roundName = roundName;
        this.winnerKey = winnerKey;
        this.loserKey = loserKey;
        this.winnerName = winnerName;
        this.loserName = loserName;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
        this.isBye = isBye;
    }


}
