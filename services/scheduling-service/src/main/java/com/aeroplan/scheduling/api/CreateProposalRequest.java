package com.aeroplan.scheduling.api;

import java.util.Map;

public record CreateProposalRequest(
        String taskToken,
        String executionArn,
        Long machineDowntimeId,
        Long basedOnScheduleVersionId,
        Long candidateScheduleVersionId,
        String agentSummary,
        Map<String, Object> agentFindings) {
}
