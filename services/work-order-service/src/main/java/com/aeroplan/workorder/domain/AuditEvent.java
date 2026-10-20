package com.aeroplan.workorder.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** `detail` holds serialized JSON (via Jackson at the service layer), stored as TEXT for
 * portability across H2 and PostgreSQL — see V1__init.sql. */
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
