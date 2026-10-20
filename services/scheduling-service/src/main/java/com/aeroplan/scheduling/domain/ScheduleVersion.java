package com.aeroplan.scheduling.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Immutable once published; a new plan is always a new version (never mutated in place). */
@Entity
@Table(name = "schedule_version")
public class ScheduleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_number", nullable = false, unique = true)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduleVersionStatus status;

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "objective_score")
    private Double objectiveScore;

    protected ScheduleVersion() {
    }

    public ScheduleVersion(int versionNumber, ScheduleVersionStatus status, Double objectiveScore) {
        this.versionNumber = versionNumber;
        this.status = status;
        this.objectiveScore = objectiveScore;
    }

    public Long id() {
        return id;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public ScheduleVersionStatus status() {
        return status;
    }

    public void status(ScheduleVersionStatus status) {
        this.status = status;
    }

    public boolean current() {
        return current;
    }

    public void current(boolean current) {
        this.current = current;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String approvedBy() {
        return approvedBy;
    }

    public void approvedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Double objectiveScore() {
        return objectiveScore;
    }
}
