package com.aeroplan.scheduling.gateway;

import java.util.Optional;

/**
 * Mirrors the DynamoDB {@code WorkflowState} table from docs/data-model.md: lets approve/reject
 * find the right Step Functions task token without Postgres needing to know Step Functions
 * internals. Postgres ({@code schedule_proposal.step_functions_task_token}) remains the
 * authoritative copy (ADR-001's pragmatic denormalization) — this store is supplementary.
 */
public interface WorkflowStateStore {
    void put(String executionArn, Long proposalId, String taskToken);

    Optional<String> findTaskToken(String executionArn);
}
