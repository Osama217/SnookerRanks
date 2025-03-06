package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.PlayerDTO;
import com.egr.snookerrank.dto.PlayerPrizeStatsDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDetailsDTO {
    PlayerDTO player;
    List<PlayerPrizeStatsDTO> prizeStats;

}
