package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.PlayerMatchTournamentDTO;
import com.egr.snookerrank.dto.PlayerPrizeTournamentDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TournamentStatsDTO {
    private List<PlayerMatchTournamentDTO> matches;
    private List<PlayerPrizeTournamentDTO> prizes;

}
