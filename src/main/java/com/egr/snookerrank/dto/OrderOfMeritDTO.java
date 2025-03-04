package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import  java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderOfMeritDTO {
    private Integer playerKey;
    private String playerName;
    private String countryName;
    private Double age;
    private String dob;
    private Boolean isWoman;
    private Double sumPrizeMoney;
    private Integer totalEvents;
    private Integer inProgress;
}
