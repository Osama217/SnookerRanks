package com.egr.snookerrank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TournamnetStats {
    Integer playerKey;
    String playerName;
    Integer year;
    Integer count;
    String roundLabel;

}
