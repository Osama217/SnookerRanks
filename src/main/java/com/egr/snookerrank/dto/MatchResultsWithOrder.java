package com.egr.snookerrank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchResultsWithOrder {
    private Integer tournamentKey;   // t.tournament_key
    private Integer eventKey;        // e.event_key
    private String tournamentName; // t.tournament_name
    private Date eventDate;       // e.event_date
    private String eventCategory; // e.event_category
    private String roundName;     // r.round_name
    @JsonProperty("opponentKey")
    private Integer winnerKey;       // m.winner_key
    private Integer loserKey;        // m.loser_key
    @JsonProperty("opponent")
    private String winnerName;    // w.player_name
    private String loserName;     // l.player_name
    private Integer winnerScore;  // m.winner_score
    private Integer loserScore;   // m.loser_score
    private String score;
    private String result;
    public MatchResultsWithOrder(Integer tournamentKey, Integer eventKey, String tournamentName, Date eventDate, String eventCategory, String roundName, Integer winnerKey, Integer loserKey, String winnerName, String loserName, Integer winnerScore, Integer loserScore) {
        this.tournamentKey = tournamentKey;
        this.eventKey = eventKey;
        this.tournamentName = tournamentName;
        this.eventDate = eventDate;
        this.eventCategory = eventCategory;
        this.roundName = roundName;
        this.winnerKey = winnerKey;
        this.loserKey = loserKey;
        this.winnerName = winnerName;
        this.loserName = loserName;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
    }
}
