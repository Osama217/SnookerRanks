package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.PlayerH2HStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class H2HListDTO {
    String name;
    List<PlayerH2HStatsDTO> playerH2HStats;
}
