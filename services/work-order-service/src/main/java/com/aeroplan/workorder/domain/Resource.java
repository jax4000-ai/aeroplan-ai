package com.aeroplan.workorder.domain;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * `skills` is persisted as a comma-separated string (not a native array type) so the same
 * mapping works against both H2 (local dev) and PostgreSQL (production) — see the note in
 * V1__init.sql / docs/data-model.md.
 */
@Entity
@Table(name = "resource")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(nullable = false, length = 500)
    private String skills = "";

    @Column(nullable = false)
    private int capacity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 16)
    private AvailabilityStatus availabilityStatus;

    protected Resource() {
    }

    public Resource(String name, String resourceType, List<String> skills, int capacity, AvailabilityStatus availabilityStatus) {
        this.name = name;
        this.resourceType = resourceType;
        this.skills = String.join(",", skills);
        this.capacity = capacity;
        this.availabilityStatus = availabilityStatus;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String resourceType() {
        return resourceType;
    }

    public List<String> skills() {
        return skills == null || skills.isBlank() ? List.of() : Arrays.asList(skills.split(","));
    }

    public int capacity() {
        return capacity;
    }

    public AvailabilityStatus availabilityStatus() {
        return availabilityStatus;
    }
}
