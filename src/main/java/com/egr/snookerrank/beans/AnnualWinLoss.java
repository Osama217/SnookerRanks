package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnualWinLoss {
    private Integer year;
    private Integer wins;
    private Integer losses;
    private Integer legsWon;
    private Integer legsLost;
    private Integer matches;


}
