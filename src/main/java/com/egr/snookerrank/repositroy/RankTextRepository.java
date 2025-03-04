package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.model.RankText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankTextRepository extends JpaRepository<RankText, Integer> {
    RankText findByRankTextKey(int rankTextKey);
}
