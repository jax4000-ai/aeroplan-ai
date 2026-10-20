package com.aeroplan.workorder.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rocket_id", nullable = false)
    private Rocket rocket;

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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceIndex ASC")
    private List<Operation> operations = new ArrayList<>();

    protected WorkOrder() {
    }

    public WorkOrder(Rocket rocket, String partNumber, String title, WorkType workType, Priority priority, Instant dueAt, WorkOrderStatus status, Instant now) {
        this.rocket = rocket;
        this.partNumber = partNumber;
        this.title = title;
        this.workType = workType;
        this.priority = priority;
        this.dueAt = dueAt;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long id() {
        return id;
    }

    public Rocket rocket() {
        return rocket;
    }

    public String partNumber() {
        return partNumber;
    }

    public String title() {
        return title;
    }

    public WorkType workType() {
        return workType;
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

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
    }

    public List<Operation> operations() {
        return operations;
    }

    public void addOperation(Operation operation) {
        operations.add(operation);
        operation.assignTo(this);
    }

    public void updateStatus(WorkOrderStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
