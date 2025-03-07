package com.egr.snookerrank.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TournamentStats {
    LocalDate tournamentDate;
    String tournamentName;
    String category;
    String round;
    String opponentName;
    String score;

    public TournamentStats(LocalDate tournamentDate, String tournamentName, String category, String round) {
        this.tournamentDate = tournamentDate;
        this.tournamentName = tournamentName;
        this.category = category;
        this.round = round;
    }
}
