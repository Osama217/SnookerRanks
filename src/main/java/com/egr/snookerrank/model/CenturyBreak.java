package com.egr.snookerrank.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "century_break")
@Data
public class CenturyBreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "century_break_key")
    private Double centuryBreakKey;

    @Column(name = "event_key")
    private Double eventKey;

    @Column(name = "player_key")
    private Double playerKey;

    @Column(name = "total_break")
    private Double totalBreak;

    @Column(name = "in_qualifying")
    private Double inQualifying;
}

