package com.aeroplan.scheduling.engine;

import java.util.List;
import java.util.Map;

/**
 * The output of one scheduling-engine run: a candidate plan plus the feasibility report used to
 * score and compare it against other candidates (see {@code GET /api/schedules/optimize} in
 * docs/api-specification.md).
 */
public record ScheduleCandidateResult(
        String label,
        double objectiveScore,
        Map<Long, Long> lateWorkOrderHours,
        long overtimeMinutesUsed,
        int changedAssignments,
        List<AssignmentDraft> assignments) {
}
