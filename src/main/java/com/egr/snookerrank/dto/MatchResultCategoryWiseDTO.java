package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResultCategoryWiseDTO {
    private String category;
    private Integer player1Wins;
    private Integer draws;
    private Integer player2Wins;
}
