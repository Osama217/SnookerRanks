package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.CorrectScoreDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.Pair;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class H2HChancesToWinDTO {
    Pair<Double,Double> chancesToWin;
    List<CorrectScoreDTO> correctScoreDTOS;

}
