package com.aeroplan.scheduling.api;

import java.time.Instant;
import java.util.List;

import com.aeroplan.scheduling.service.PersistedCandidate;

public record CandidateResponse(
        Long scheduleVersionId,
        String label,
        double objectiveScore,
        List<LateWorkOrderResponse> lateWorkOrders,
        long overtimeMinutesUsed,
        int changedAssignments,
        List<AssignmentDraftResponse> assignments) {

    public record LateWorkOrderResponse(Long workOrderId, long lateHours) {
    }

    public record AssignmentDraftResponse(Long operationId, Long resourceId, Instant startAt, Instant endAt) {
    }

    public static CandidateResponse from(PersistedCandidate candidate) {
        List<LateWorkOrderResponse> late = candidate.result().lateWorkOrderHours().entrySet().stream()
                .map(e -> new LateWorkOrderResponse(e.getKey(), e.getValue()))
                .toList();
        List<AssignmentDraftResponse> assignments = candidate.result().assignments().stream()
                .map(a -> new AssignmentDraftResponse(a.operationId(), a.resourceId(), a.startAt(), a.endAt()))
                .toList();
        return new CandidateResponse(candidate.scheduleVersionId(), candidate.result().label(),
                candidate.result().objectiveScore(), late, candidate.result().overtimeMinutesUsed(),
                candidate.result().changedAssignments(), assignments);
    }
}
