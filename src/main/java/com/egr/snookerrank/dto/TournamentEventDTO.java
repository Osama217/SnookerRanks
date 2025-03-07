package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentEventDTO {
    private Integer tournamentKey;
    private String tournamentName;
    private Integer eventDate;
    private String status = "Winner";

    public TournamentEventDTO(Integer tournamentKey, String tournamentName, Integer eventDate) {
        this.tournamentKey = tournamentKey;
        this.tournamentName = tournamentName;
        this.eventDate = eventDate;
    }
}
