package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDTO {
    List<RanksDTO> ranksList;
    List<TournamentDTO> tournamentTypes;
    List<Integer> tourCardYear;

}
