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

/** Read-only mirror of the `work_order` table owned by work-order-service. */
@Entity
@Table(name = "work_order")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rocket_id", nullable = false)
    private Long rocketId;

    @Column(name = "part_number", nullable = false, length = 64)
    private String partNumber;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 32)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Priority priority;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkOrderStatus status;

    protected WorkOrder() {
    }

    public WorkOrder(Long rocketId, String partNumber, String title, WorkType workType, Priority priority,
            Instant dueAt, WorkOrderStatus status) {
        this.rocketId = rocketId;
        this.partNumber = partNumber;
        this.title = title;
        this.workType = workType;
        this.priority = priority;
        this.dueAt = dueAt;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public Long rocketId() {
        return rocketId;
    }

    public String partNumber() {
        return partNumber;
    }

    public String title() {
        return title;
    }

    public Priority priority() {
        return priority;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public WorkOrderStatus status() {
        return status;
    }
}
