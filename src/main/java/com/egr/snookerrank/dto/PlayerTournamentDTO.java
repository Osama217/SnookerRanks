package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerTournamentDTO {
    private Integer tournamentKey;
    private String tournamentName;
    private Integer prestige;
    private Integer roundNo;
    private String roundName;
    private List<Integer> years;

    public PlayerTournamentDTO(Integer tournamentKey, String tournamentName, Integer prestige, Integer roundNo, String roundName){
        this.tournamentKey = tournamentKey;
        this.tournamentName = tournamentName;
        this.prestige = prestige;
        this.roundNo = roundNo;
        this.roundName = roundName;
    }
}
