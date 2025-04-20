package com.egr.snookerrank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerDTO {
    private Integer playerKey;
    private String playerName;
    private String forename;
    private String surname;
    private String countryName;
    private Number fdi;
    private Integer fdiMatches;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime fdiLastDate;
    private Double tournRank;
    private Double fdiRank;
    private Boolean isSemiPro;
    private Boolean isPro;
    private Double tourCardYears;
    private String nickname;
    private String website;
    private Double age;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime dob;
    private String homeTown;
    private String formerJob;
    private String mainSponsor;
    private String wifesName;
    private String laterality;
    private String maidenName;
    private Boolean isWoman;
    private Double grading;
    private Double gradingMatches;
    private Double yearGrading;
    private Double yearGradingMatches;
    private Double sdbRanking;
    private Boolean isProfessional;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime dateOfDeath;
    private Double worldRank;
    private Double mostAccessRank;
    private Double mostAccessYearRank;
    private String aka;
    private Double hallOfFame;
    private Double isMember;
    private String memberStylesheet;
   private String biogLink;
    private String biogPictureLink;
    private String contactEmailAddress;
    private Double piosRank;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime proStartDate;
    private Integer career147s;
    private Integer careerCenturies;


    public PlayerDTO(Integer playerKey, String playerName, Double fdi) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.fdi = fdi;
    }

    public PlayerDTO(Integer playerKey, String playerName, Double fdi, String countryName, Double age) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.fdi = fdi;
        this.countryName = countryName;
        this.age = age;
    }

    public PlayerDTO(Integer playerKey, String playerName, String countryName, Double fdi) {
        this.playerKey = playerKey;
        this.playerName = playerName;
        this.countryName = countryName;
        this.fdi = fdi;
    }
}
