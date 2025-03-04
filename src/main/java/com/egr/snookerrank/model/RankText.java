package com.egr.snookerrank.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "rank_text")
public class RankText {

    @Id
    @Column(name = "rank_text_key")
    private int rankTextKey;

    @Column(name = "rank_name", nullable = false, length = 100)
    private String rankName;

    @Column(name = "rank_text", nullable = false, length = 250)
    private String rankText;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_match_stat", nullable = false)
    private boolean isMatchStat;

    @Column(name = "stat_type")
    private String statType;

    @Column(name = "order_asc", nullable = false)
    private boolean orderAsc;

    @Column(name = "field1")
    private String field1;

    @Column(name = "field2")
    private String field2;

    @Column(name = "order_num")
    private Double orderNum;

    @Column(name = "is_talent_portal", nullable = false)
    private boolean isTalentPortal;

    @Column(name = "talent_portal_cat")
    private String talentPortalCat;

    @Column(name = "talent_portal_order")
    private Double talentPortalOrder;

    @Column(name = "min_for_season_top20")
    private Double minForSeasonTop20;

    @Column(name = "is_ranking", nullable = false)
    private boolean isRanking;
}

