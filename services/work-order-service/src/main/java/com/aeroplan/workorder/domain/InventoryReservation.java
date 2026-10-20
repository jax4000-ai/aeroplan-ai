package com.aeroplan.workorder.domain;

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

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private Operation operation;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(WorkOrder workOrder, Material material, Operation operation, int quantity, ReservationStatus status, Instant reservedAt) {
        this.workOrder = workOrder;
        this.material = material;
        this.operation = operation;
        this.quantity = quantity;
        this.status = status;
        this.reservedAt = reservedAt;
    }

    public Long id() {
        return id;
    }

    public WorkOrder workOrder() {
        return workOrder;
    }

    public Material material() {
        return material;
    }

    public Operation operation() {
        return operation;
    }

    public int quantity() {
        return quantity;
    }

    public ReservationStatus status() {
        return status;
    }

    public Instant reservedAt() {
        return reservedAt;
    }
}
