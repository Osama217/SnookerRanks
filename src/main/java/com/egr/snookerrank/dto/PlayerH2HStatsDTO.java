package com.egr.snookerrank.dto;

public class PlayerH2HStatsDTO {
    private Integer playerKey;
    private String playerName;
    private Integer wins;
    private Integer draw;
    private Integer losses;
    private Double pcnt;

    // Constructor
    public PlayerH2HStatsDTO(Integer playerKey, String playerName, Integer wins, Integer draw, Integer losses, Double pcnt) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.wins = wins;
        this.draw = draw;
        this.losses = losses;
        this.pcnt = pcnt;
    }

    // Getters and Setters
    public Integer getPlayerKey() {
        return playerKey;
    }

    public void setPlayerKey(Integer playerKey) {
        this.playerKey = playerKey;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getDraw() {
        return draw;
    }

    public void setDraw(Integer draw) {
        this.draw = draw;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Double getPcnt() {
        return pcnt;
    }

    public void setPcnt(Double pcnt) {
        this.pcnt = pcnt;
    }
}
