package com.egr.snookerrank.repositroy;

import com.egr.snookerrank.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    Optional<WaitlistEntry> findByEmail(String email);
}