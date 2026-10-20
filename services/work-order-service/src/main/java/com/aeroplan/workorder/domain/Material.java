package com.aeroplan.workorder.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "material")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_number", nullable = false, unique = true, length = 64)
    private String partNumber;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_on_order", nullable = false)
    private int quantityOnOrder;

    @Column(length = 120)
    private String supplier;

    @Column(name = "expected_at")
    private Instant expectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MaterialStatus status;

    protected Material() {
    }

    public Material(String partNumber, String description, int quantityOnHand, int quantityOnOrder, String supplier, Instant expectedAt, MaterialStatus status) {
        this.partNumber = partNumber;
        this.description = description;
        this.quantityOnHand = quantityOnHand;
        this.quantityOnOrder = quantityOnOrder;
        this.supplier = supplier;
        this.expectedAt = expectedAt;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public String partNumber() {
        return partNumber;
    }

    public String description() {
        return description;
    }

    public int quantityOnHand() {
        return quantityOnHand;
    }

    public int quantityOnOrder() {
        return quantityOnOrder;
    }

    public String supplier() {
        return supplier;
    }

    public Instant expectedAt() {
        return expectedAt;
    }

    public MaterialStatus status() {
        return status;
    }
}
