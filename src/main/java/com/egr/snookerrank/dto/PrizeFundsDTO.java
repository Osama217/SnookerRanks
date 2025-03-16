package com.egr.snookerrank.dto;

import com.egr.snookerrank.beans.MatchResults;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrizeFundsDTO {
    private String roundName;
    private Double roundNo;
    private Number prizeMoney;
    private Integer numberOfMatches;
    private String countryName;
    private Boolean isLeague;
    private Boolean isGroup;
    List<MatchResults> matchResults;

    public PrizeFundsDTO(String roundName,Number prizeMoney){
        this.roundName = roundName;
        this.prizeMoney = prizeMoney;
    }
}