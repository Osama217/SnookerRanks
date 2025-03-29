package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankFields {
    private String rankName;
    private Integer rankKey;
    private String statType;
    private String field1;
    private String field2;
}
