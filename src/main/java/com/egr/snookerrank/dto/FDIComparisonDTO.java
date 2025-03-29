package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FDIComparisonDTO {
    private  Integer playerKey;
    private  String playerName;
    private Number age;
    private Number fdiRank;
    private Number worldRank;
    private String countryName;
    private Number diff;

}
