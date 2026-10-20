package com.aeroplan.scheduling.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * The output of a disruption-recovery Step Functions execution, paused on a planner's
 * approve/reject decision. {@code agentFindings} is persisted as a JSON string (TEXT column, not
 * native JSONB) for H2/PostgreSQL portability — see the equivalent note on work-order-service's
 * schema deviations.
 */
@Entity
@Table(name = "schedule_proposal")
public class ScheduleProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_downtime_id")
    private Long machineDowntimeId;

    @Column(name = "based_on_schedule_version_id", nullable = false)
    private Long basedOnScheduleVersionId;

    @Column(name = "candidate_schedule_version_id")
    private Long candidateScheduleVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Lob
    @Column(name = "agent_summary")
    private String agentSummary;

    @Lob
    @Column(name = "agent_findings", nullable = false)
    private String agentFindings = "{}";

    @Column(name = "step_functions_task_token")
    private String stepFunctionsTaskToken;

    @Column(name = "step_functions_execution_arn")
    private String stepFunctionsExecutionArn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 80)
    private String decidedBy;

    protected ScheduleProposal() {
    }

    public ScheduleProposal(Long machineDowntimeId, Long basedOnScheduleVersionId, Long candidateScheduleVersionId,
            String agentSummary, String agentFindings, String stepFunctionsTaskToken, String stepFunctionsExecutionArn) {
        this.machineDowntimeId = machineDowntimeId;
        this.basedOnScheduleVersionId = basedOnScheduleVersionId;
        this.candidateScheduleVersionId = candidateScheduleVersionId;
        this.agentSummary = agentSummary;
        this.agentFindings = agentFindings == null ? "{}" : agentFindings;
        this.stepFunctionsTaskToken = stepFunctionsTaskToken;
        this.stepFunctionsExecutionArn = stepFunctionsExecutionArn;
    }

    public Long id() {
        return id;
    }

    public Long machineDowntimeId() {
        return machineDowntimeId;
    }

    public Long basedOnScheduleVersionId() {
        return basedOnScheduleVersionId;
    }

    public Long candidateScheduleVersionId() {
        return candidateScheduleVersionId;
    }

    public ProposalStatus status() {
        return status;
    }

    public void status(ProposalStatus status) {
        this.status = status;
    }

    public String agentSummary() {
        return agentSummary;
    }

    public String agentFindings() {
        return agentFindings;
    }

    public String stepFunctionsTaskToken() {
        return stepFunctionsTaskToken;
    }

    public String stepFunctionsExecutionArn() {
        return stepFunctionsExecutionArn;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant decidedAt() {
        return decidedAt;
    }

    public void decidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String decidedBy() {
        return decidedBy;
    }

    public void decidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }
}
