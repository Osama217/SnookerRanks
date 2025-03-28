package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.MatchResultCategoryWiseDTO;
import com.egr.snookerrank.dto.MatchResultsWithOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Head2HeadchancesToWinmatchResultDTO {
    List<MatchResultCategoryWiseDTO> recentMeetings;
    List<MatchResultsWithOrder> statswithOrder;

}
