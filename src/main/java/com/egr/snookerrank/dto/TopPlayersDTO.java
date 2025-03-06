package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopPlayersDTO {
    private Integer rank;
    private Integer playerKey;
    private String playerName;
    private String countryName;
    private BigDecimal fdi;


}
