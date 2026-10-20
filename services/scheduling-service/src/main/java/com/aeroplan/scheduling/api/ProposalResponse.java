package com.aeroplan.scheduling.api;

import java.time.Instant;
import java.util.Map;

import com.aeroplan.scheduling.domain.MachineDowntime;
import com.aeroplan.scheduling.domain.ScheduleProposal;

public record ProposalResponse(
        Long id,
        String status,
        MachineDowntimeSummary machineDowntime,
        Long basedOnScheduleVersionId,
        Long candidateScheduleVersionId,
        String agentSummary,
        Map<String, Object> agentFindings,
        Instant createdAt,
        Instant decidedAt,
        String decidedBy) {

    public record MachineDowntimeSummary(String eventId, Long resourceId, String reason) {
        public static MachineDowntimeSummary from(MachineDowntime downtime) {
            return downtime == null ? null : new MachineDowntimeSummary(downtime.eventId(), downtime.resourceId(), downtime.reason());
        }
    }

    public static ProposalResponse from(ScheduleProposal proposal, MachineDowntime downtime, Map<String, Object> parsedFindings) {
        return new ProposalResponse(proposal.id(), proposal.status().name(), MachineDowntimeSummary.from(downtime),
                proposal.basedOnScheduleVersionId(), proposal.candidateScheduleVersionId(), proposal.agentSummary(),
                parsedFindings, proposal.createdAt(), proposal.decidedAt(), proposal.decidedBy());
    }
}
