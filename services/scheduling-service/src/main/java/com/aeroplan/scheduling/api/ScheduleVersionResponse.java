package com.aeroplan.scheduling.api;

import java.time.Instant;
import java.util.List;

import com.aeroplan.scheduling.domain.ScheduleAssignment;
import com.aeroplan.scheduling.domain.ScheduleVersion;

public record ScheduleVersionResponse(
        Long id,
        int versionNumber,
        String status,
        boolean current,
        Instant createdAt,
        String approvedBy,
        Double objectiveScore,
        List<AssignmentResponse> assignments) {

    public static ScheduleVersionResponse from(ScheduleVersion version, List<ScheduleAssignment> assignments) {
        return new ScheduleVersionResponse(version.id(), version.versionNumber(), version.status().name(),
                version.current(), version.createdAt(), version.approvedBy(), version.objectiveScore(),
                assignments.stream().map(AssignmentResponse::from).toList());
    }
}
