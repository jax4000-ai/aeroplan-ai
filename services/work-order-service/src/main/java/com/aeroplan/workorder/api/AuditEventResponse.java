package com.aeroplan.workorder.api;

import com.aeroplan.workorder.domain.AuditEvent;

public record AuditEventResponse(
        Long id,
        String eventType,
        String entityType,
        Long entityId,
        String actor,
        String detail,
        String occurredAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.id(), event.eventType(), event.entityType(), event.entityId(), event.actor(), event.detail(), event.occurredAt().toString());
    }
}
