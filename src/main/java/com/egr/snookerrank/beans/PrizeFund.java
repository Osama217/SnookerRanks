package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrizeFund {
    private String roundName;
    private Double roundNo;
    private Number prizeMoney;
    private Integer numberOfMatches;
    private String countryName;
    private Boolean isLeague;
    private Boolean isGroup;
}
