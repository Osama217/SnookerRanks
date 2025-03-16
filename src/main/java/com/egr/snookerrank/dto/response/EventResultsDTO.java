package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.beans.MatchResults;
import com.egr.snookerrank.beans.PrizeFund;
import com.egr.snookerrank.dto.PrizeFundsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class EventResultsDTO {
    String tournamnetName;
    List<PrizeFundsDTO> prizeFunds;
}
