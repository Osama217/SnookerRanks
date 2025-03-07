package com.egr.snookerrank.dto.response;

import com.egr.snookerrank.dto.AnnualWinLossDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerAdditionalDetailsDTO {
    private List<AnnualWinLossDTO> annualWinLoss;
}
