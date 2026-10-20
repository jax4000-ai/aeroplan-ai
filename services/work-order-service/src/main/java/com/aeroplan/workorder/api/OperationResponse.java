package com.aeroplan.workorder.api;

import java.util.List;

import com.aeroplan.workorder.domain.Operation;

public record OperationResponse(
        Long id,
        Long workOrderId,
        String name,
        int durationMinutes,
        String requiredSkill,
        String requiredResourceType,
        int sequenceIndex,
        String status,
        List<Long> dependsOnOperationIds) {

    public static OperationResponse from(Operation operation, List<Long> dependsOnOperationIds) {
        return new OperationResponse(
                operation.id(),
                operation.workOrder() == null ? null : operation.workOrder().id(),
                operation.name(),
                operation.durationMinutes(),
                operation.requiredSkill(),
                operation.requiredResourceType(),
                operation.sequenceIndex(),
                operation.status().name(),
                dependsOnOperationIds);
    }
}
