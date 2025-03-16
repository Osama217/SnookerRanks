package com.egr.snookerrank.beans;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResults {
    private Integer matchKey;
    private Double eventKey;
    private Integer matchDateYear;
    private Double roundNo;
    private Integer winnerKey;
    private Integer loserKey;
    private Integer winnerScore;
    private Integer loserScore;
    private Boolean isBye;
    private String groupLetter;
    private Boolean fdiCalcDone;
    private String playerWinnerName;
    private Integer playerWinnerKey;
    private String playerLosserName;
    private Integer playerLosserKey;


}
