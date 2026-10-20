package com.aeroplan.scheduling.api;

import java.time.Instant;

import com.aeroplan.scheduling.domain.ScheduleAssignment;

public record AssignmentResponse(Long operationId, Long resourceId, Instant startAt, Instant endAt, String status) {
    public static AssignmentResponse from(ScheduleAssignment a) {
        return new AssignmentResponse(a.operationId(), a.resourceId(), a.startAt(), a.endAt(), a.status().name());
    }
}
