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

/** Read-only mirror of the `machine_downtime` table owned by event-processing-service. */
@Entity
@Table(name = "machine_downtime")
public class MachineDowntime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 80)
    private String eventId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "estimated_recovery_at")
    private Instant estimatedRecoveryAt;

    @Column(length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DowntimeStatus status = DowntimeStatus.OPEN;

    protected MachineDowntime() {
    }

    public MachineDowntime(String eventId, Long resourceId, Instant startedAt, Instant estimatedRecoveryAt,
            String reason, DowntimeStatus status) {
        this.eventId = eventId;
        this.resourceId = resourceId;
        this.startedAt = startedAt;
        this.estimatedRecoveryAt = estimatedRecoveryAt;
        this.reason = reason;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public String eventId() {
        return eventId;
    }

    public Long resourceId() {
        return resourceId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant estimatedRecoveryAt() {
        return estimatedRecoveryAt;
    }

    public String reason() {
        return reason;
    }

    public DowntimeStatus status() {
        return status;
    }
}
