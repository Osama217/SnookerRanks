package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentPotralMatchStats {
    private Integer potsMade;
    private Integer potsAttempted;
    private Integer safetyMade;
    private Integer safetyAttempted;
    private Integer longPotsMade;
    private Integer longPotsAttempted;
    private Integer timeOnTable;
    private Integer shotsTaken;
    private Integer centuryBreaks;
    private Integer fiftyBreaks;
    private Integer seventyBreaks;
    private Integer framesWon;
    private Integer framesPlayed;
    private Integer maxBreaks;
    private Integer highestBreak;
    private Integer poIntegersScored;
}
