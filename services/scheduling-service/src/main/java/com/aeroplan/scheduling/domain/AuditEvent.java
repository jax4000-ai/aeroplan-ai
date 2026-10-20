package com.aeroplan.scheduling.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Append-only shared table (owned by work-order-service; every service writes via its own
 * mapping, there being no shared client library yet). `detail` holds serialized JSON, stored as
 * TEXT for H2/PostgreSQL portability.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 80)
    private String actor;

    @Lob
    @Column(nullable = false)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventType, String entityType, Long entityId, String actor, String detail, Instant occurredAt) {
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actor = actor;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long id() {
        return id;
    }

    public String eventType() {
        return eventType;
    }

    public String entityType() {
        return entityType;
    }

    public Long entityId() {
        return entityId;
    }

    public String actor() {
        return actor;
    }

    public String detail() {
        return detail;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
