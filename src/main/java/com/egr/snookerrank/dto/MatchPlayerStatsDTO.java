package com.egr.snookerrank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class MatchPlayerStatsDTO {

   Map<String,String> winnerMap;
   Map<String,String> losserMap;



}
