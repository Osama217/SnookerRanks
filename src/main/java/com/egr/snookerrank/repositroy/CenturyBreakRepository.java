package com.egr.snookerrank.repositroy;


import com.egr.snookerrank.model.CenturyBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenturyBreakRepository extends JpaRepository<CenturyBreak, Double> {
}
