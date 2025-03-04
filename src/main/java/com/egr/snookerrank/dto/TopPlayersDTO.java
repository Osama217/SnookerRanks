package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopPlayersDTO {
    private Integer playerKey;
    private String playerName;
    private String countryName;
    private Double fdi;

}
