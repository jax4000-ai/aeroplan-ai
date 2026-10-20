package com.aeroplan.scheduling.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "schedule_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_version_id", "operation_id"}))
public class ScheduleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_version_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleVersion scheduleVersion;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentStatus status = AssignmentStatus.SCHEDULED;

    protected ScheduleAssignment() {
    }

    public ScheduleAssignment(ScheduleVersion scheduleVersion, Long operationId, Long resourceId, Instant startAt, Instant endAt) {
        this.scheduleVersion = scheduleVersion;
        this.operationId = operationId;
        this.resourceId = resourceId;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long id() {
        return id;
    }

    public ScheduleVersion scheduleVersion() {
        return scheduleVersion;
    }

    public Long operationId() {
        return operationId;
    }

    public Long resourceId() {
        return resourceId;
    }

    public Instant startAt() {
        return startAt;
    }

    public Instant endAt() {
        return endAt;
    }

    public AssignmentStatus status() {
        return status;
    }
}
