package com.aeroplan.scheduling.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Latest launch-readiness rollup per rocket. Recomputed on each read; this row is the cache. */
@Entity
@Table(name = "launch_preparation")
public class LaunchPreparation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rocket_id", nullable = false)
    private Long rocketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 20)
    private ReadinessStatus readinessStatus;

    @Column(name = "readiness_score")
    private Double readinessScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_status", length = 20)
    private WeatherStatus weatherStatus;

    @Column(name = "planned_launch_at")
    private Instant plannedLaunchAt;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    @Lob
    private String notes;

    protected LaunchPreparation() {
    }

    public LaunchPreparation(Long rocketId, ReadinessStatus readinessStatus, Double readinessScore,
            WeatherStatus weatherStatus, Instant plannedLaunchAt, String notes) {
        this.rocketId = rocketId;
        this.readinessStatus = readinessStatus;
        this.readinessScore = readinessScore;
        this.weatherStatus = weatherStatus;
        this.plannedLaunchAt = plannedLaunchAt;
        this.notes = notes;
    }

    public Long id() {
        return id;
    }

    public Long rocketId() {
        return rocketId;
    }

    public ReadinessStatus readinessStatus() {
        return readinessStatus;
    }

    public Double readinessScore() {
        return readinessScore;
    }

    public WeatherStatus weatherStatus() {
        return weatherStatus;
    }

    public Instant plannedLaunchAt() {
        return plannedLaunchAt;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }

    public String notes() {
        return notes;
    }
}
