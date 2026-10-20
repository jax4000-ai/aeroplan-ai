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

/**
 * Read-only mirror of the `rocket` table owned by work-order-service. scheduling-service never
 * writes these rows in production (shared Postgres schema); in the local/demo H2 profile this
 * service seeds its own copy of the same demo scenario so it can be exercised standalone.
 */
@Entity
@Table(name = "rocket")
public class Rocket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 80)
    private String variant;

    @Column(length = 120)
    private String configuration;

    @Column(name = "launch_window_start")
    private Instant launchWindowStart;

    @Column(name = "launch_window_end")
    private Instant launchWindowEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RocketStatus status;

    protected Rocket() {
    }

    public Rocket(String name, String variant, String configuration, Instant launchWindowStart,
            Instant launchWindowEnd, RocketStatus status) {
        this.name = name;
        this.variant = variant;
        this.configuration = configuration;
        this.launchWindowStart = launchWindowStart;
        this.launchWindowEnd = launchWindowEnd;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String variant() {
        return variant;
    }

    public Instant launchWindowStart() {
        return launchWindowStart;
    }

    public Instant launchWindowEnd() {
        return launchWindowEnd;
    }

    public RocketStatus status() {
        return status;
    }
}
