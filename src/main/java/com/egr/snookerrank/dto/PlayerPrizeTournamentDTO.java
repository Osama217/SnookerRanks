package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerPrizeTournamentDTO {

    private Integer tournamentKey;
    private Integer eventKey;
    private String tournamentName;
    private Integer tournamentNo;
    private Date eventDate;
    private String eventCategory;
    private BigDecimal prizeMoney;
    private String roundName;
    private Boolean fullPrizes;
    private String currencyCode;


}
