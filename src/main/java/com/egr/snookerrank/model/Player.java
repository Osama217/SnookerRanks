package com.egr.snookerrank.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "player")
@Data
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_key")
    private Integer playerKey;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "forename")
    private String forename;

    @Column(name = "surname")
    private String surname;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "fdi")
    private Double fdi;

    @Column(name = "fdi_matches")
    private Integer fdiMatches;

    @Column(name = "fdi_last_date")
    private LocalDateTime fdiLastDate;

    @Column(name = "tourn_rank")
    private Double tournRank;

    @Column(name = "fdi_rank")
    private Double fdiRank;

    @Column(name = "is_semi_pro")
    private Boolean isSemiPro;

    @Column(name = "is_pro")
    private Boolean isPro;

    @Column(name = "tour_card_years")
    private Double tourCardYears;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "website")
    private String website;

    @Column(name = "age")
    private Double age;

    @Column(name = "dob")
    private LocalDateTime dob;

    @Column(name = "home_town")
    private String homeTown;

    @Column(name = "former_job")
    private String formerJob;

    @Column(name = "main_sponsor")
    private String mainSponsor;

    @Column(name = "wifes_name")
    private String wifesName;

    @Column(name = "laterality")
    private String laterality;

    @Column(name = "maiden_name")
    private String maidenName;

    @Column(name = "is_woman")
    private Boolean isWoman;

    @Column(name = "grading")
    private Double grading;

    @Column(name = "grading_matches")
    private Double gradingMatches;

    @Column(name = "year_grading")
    private Double yearGrading;

    @Column(name = "year_grading_matches")
    private Double yearGradingMatches;

    @Column(name = "sdb_ranking")
    private Double sdbRanking;

    @Column(name = "is_professional")
    private Boolean isProfessional;

    @Column(name = "date_of_death")
    private LocalDateTime dateOfDeath;

    @Column(name = "world_rank")
    private Double worldRank;

    @Column(name = "most_access_rank")
    private Double mostAccessRank;

    @Column(name = "most_access_year_rank")
    private Double mostAccessYearRank;

    @Column(name = "aka")
    private String aka;

    @Column(name = "hall_of_fame")
    private Double hallOfFame;

    @Column(name = "is_member")
    private Double isMember;

    @Column(name = "member_stylesheet")
    private String memberStylesheet;

    @Column(name = "biog_link")
    private String biogLink;

    @Column(name = "biog_picture_link")
    private String biogPictureLink;

    @Column(name = "contact_email_address")
    private String contactEmailAddress;

    @Column(name = "pios_rank")
    private Double piosRank;

    @Column(name = "pro_start_date")
    private LocalDateTime proStartDate;


}