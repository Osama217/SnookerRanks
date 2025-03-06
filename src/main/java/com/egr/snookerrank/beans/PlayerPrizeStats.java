package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerPrizeStats {
    private Integer yearActive;
    private Number totalPrizeMoney;
    private Integer titlesWon;
    private Number prizePerEvent;
}
