package com.egr.snookerrank.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerPrizeStatsDTO {
    private Integer yearActive;
    private Number totalPrizeMoney;
    private Integer titlesWon;
    private Number prizePerEvent;
}
