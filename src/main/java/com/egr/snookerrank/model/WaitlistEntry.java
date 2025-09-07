package com.egr.snookerrank.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "waitlist", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    // Constructors
    public WaitlistEntry() {}

    public WaitlistEntry(String email) {
        this.email = email;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    // Getters and setters omitted for brevity
}