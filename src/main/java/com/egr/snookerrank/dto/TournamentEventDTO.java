package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentEventDTO {
    private Integer tournamentKey;
    private String tournamentName;
    private Integer eventDate;
    private String status = "Winner";
    private List<Integer> years;

    public TournamentEventDTO(Integer tournamentKey, String tournamentName, Integer eventDate) {
        this.tournamentKey = tournamentKey;
        this.tournamentName = tournamentName;
        this.eventDate = eventDate;
    }
}
