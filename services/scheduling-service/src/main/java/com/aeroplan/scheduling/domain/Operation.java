package com.aeroplan.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only mirror of the `operation` table owned by work-order-service. */
@Entity
@Table(name = "operation")
public class Operation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false)
    private Long workOrderId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "required_skill", nullable = false, length = 64)
    private String requiredSkill;

    @Column(name = "required_resource_type", nullable = false, length = 64)
    private String requiredResourceType;

    @Column(name = "sequence_index", nullable = false)
    private int sequenceIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationStatus status = OperationStatus.PLANNED;

    protected Operation() {
    }

    public Operation(Long workOrderId, String name, int durationMinutes, String requiredSkill,
            String requiredResourceType, int sequenceIndex, OperationStatus status) {
        this.workOrderId = workOrderId;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredSkill = requiredSkill;
        this.requiredResourceType = requiredResourceType;
        this.sequenceIndex = sequenceIndex;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public Long workOrderId() {
        return workOrderId;
    }

    public String name() {
        return name;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public String requiredSkill() {
        return requiredSkill;
    }

    public String requiredResourceType() {
        return requiredResourceType;
    }

    public int sequenceIndex() {
        return sequenceIndex;
    }

    public OperationStatus status() {
        return status;
    }
}
