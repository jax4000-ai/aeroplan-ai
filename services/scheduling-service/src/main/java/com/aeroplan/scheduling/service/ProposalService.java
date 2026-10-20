package com.aeroplan.scheduling.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeroplan.scheduling.common.ConflictException;
import com.aeroplan.scheduling.common.ErrorCode;
import com.aeroplan.scheduling.common.NotFoundException;
import com.aeroplan.scheduling.domain.ProposalStatus;
import com.aeroplan.scheduling.domain.ScheduleProposal;
import com.aeroplan.scheduling.gateway.NotificationClient;
import com.aeroplan.scheduling.gateway.StepFunctionsGateway;
import com.aeroplan.scheduling.gateway.WorkflowStateStore;
import com.aeroplan.scheduling.repository.ScheduleProposalRepository;

/**
 * The approval workflow at the heart of disruption recovery (ADR-002): approve/reject perform
 * their business transaction synchronously, then call SendTaskSuccess purely to close out the
 * Step Functions execution history — the API call is the authoritative action, not the callback.
 */
@Service
public class ProposalService {
    private final ScheduleProposalRepository proposalRepository;
    private final SchedulingService schedulingService;
    private final AuditService auditService;
    private final NotificationClient notificationClient;
    private final StepFunctionsGateway stepFunctionsGateway;
    private final WorkflowStateStore workflowStateStore;
    private final Clock clock;

    public ProposalService(ScheduleProposalRepository proposalRepository, SchedulingService schedulingService,
            AuditService auditService, NotificationClient notificationClient, StepFunctionsGateway stepFunctionsGateway,
            WorkflowStateStore workflowStateStore, Clock clock) {
        this.proposalRepository = proposalRepository;
        this.schedulingService = schedulingService;
        this.auditService = auditService;
        this.notificationClient = notificationClient;
        this.stepFunctionsGateway = stepFunctionsGateway;
        this.workflowStateStore = workflowStateStore;
        this.clock = clock;
    }

    @Transactional
    public Long create(Long machineDowntimeId, Long basedOnScheduleVersionId, Long candidateScheduleVersionId,
            String agentSummary, String agentFindingsJson, String taskToken, String executionArn) {
        ScheduleProposal proposal = proposalRepository.save(new ScheduleProposal(machineDowntimeId,
                basedOnScheduleVersionId, candidateScheduleVersionId, agentSummary, agentFindingsJson, taskToken,
                executionArn));
        workflowStateStore.put(executionArn, proposal.id(), taskToken);
        auditService.record("PROPOSAL_CREATED", "ScheduleProposal", proposal.id(), "step-functions",
                Map.of("machineDowntimeId", String.valueOf(machineDowntimeId), "executionArn", String.valueOf(executionArn)));
        return proposal.id();
    }

    public ScheduleProposal get(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal " + id + " was not found"));
    }

    public List<ScheduleProposal> list(ProposalStatus status) {
        return status == null ? proposalRepository.findAll() : proposalRepository.findByStatus(status);
    }

    @Transactional
    public ScheduleProposal approve(Long id, String approvedBy) {
        ScheduleProposal proposal = decide(id, ProposalStatus.APPROVED, approvedBy);
        if (proposal.candidateScheduleVersionId() != null) {
            schedulingService.publish(proposal.candidateScheduleVersionId());
        }
        auditService.record("PROPOSAL_APPROVED", "ScheduleProposal", id, approvedBy,
                Map.of("candidateScheduleVersionId", String.valueOf(proposal.candidateScheduleVersionId())));
        notificationClient.notify("PROPOSAL_APPROVED",
                "Schedule proposal #" + id + " approved by " + approvedBy + "; new schedule published.", null);
        stepFunctionsGateway.sendTaskSuccess(proposal.stepFunctionsTaskToken(), "{\"decision\":\"APPROVED\"}");
        return proposal;
    }

    @Transactional
    public ScheduleProposal reject(Long id, String rejectedBy) {
        ScheduleProposal proposal = decide(id, ProposalStatus.REJECTED, rejectedBy);
        schedulingService.markRejected(proposal.candidateScheduleVersionId());
        auditService.record("PROPOSAL_REJECTED", "ScheduleProposal", id, rejectedBy, Map.of());
        notificationClient.notify("PROPOSAL_REJECTED",
                "Schedule proposal #" + id + " rejected by " + rejectedBy + ".", null);
        stepFunctionsGateway.sendTaskSuccess(proposal.stepFunctionsTaskToken(), "{\"decision\":\"REJECTED\"}");
        return proposal;
    }

    @Transactional
    public void expire(String executionArn) {
        proposalRepository.findByStepFunctionsExecutionArn(executionArn)
                .filter(p -> p.status() == ProposalStatus.PENDING)
                .ifPresent(p -> {
                    int updated = proposalRepository.decideIfPending(p.id(), ProposalStatus.EXPIRED, clock.instant(), "step-functions-timeout");
                    if (updated == 1) {
                        auditService.record("PROPOSAL_EXPIRED", "ScheduleProposal", p.id(), "step-functions-timeout", Map.of());
                    }
                });
    }

    /** Optimistic conditional transition; throws 409 if the proposal was already decided. */
    private ScheduleProposal decide(Long id, ProposalStatus newStatus, String decidedBy) {
        int updated = proposalRepository.decideIfPending(id, newStatus, clock.instant(), decidedBy);
        if (updated == 0) {
            ScheduleProposal existing = proposalRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PROPOSAL_NOT_FOUND, "Proposal " + id + " was not found"));
            throw new ConflictException(ErrorCode.PROPOSAL_ALREADY_DECIDED,
                    "Proposal " + id + " is already " + existing.status() + " and cannot be decided again");
        }
        return get(id);
    }
}
