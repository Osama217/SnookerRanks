package com.egr.snookerrank.dto;

import lombok.*;

@Builder
@ToString
@Data
public class AnnualWinLossDTO {
    private Integer year;
    private String winsByLosses;
    private String legsWonByLegsLost;
    private String winByLossPercentage;


}
