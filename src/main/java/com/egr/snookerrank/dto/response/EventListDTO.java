package com.egr.snookerrank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventListDTO {
    private Integer eventKey;
    private String tournamentName;
    private Integer tournamentNo;
    private String eventDate;
    private String startDate;
    private String endDate;
    private String currencyCode;
    private Number prizeFund;
    private Number conversionRate;
    private Integer tournamentKey;
    private Boolean fullPrizes;
    private Integer playerKey;
    private String winnerName;
    private String eventCategory;
}
