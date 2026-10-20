package com.aeroplan.workorder.api;

import java.util.List;

import com.aeroplan.workorder.domain.WorkOrder;

public record WorkOrderResponse(
        Long id,
        Long rocketId,
        String partNumber,
        String title,
        String workType,
        String priority,
        String dueAt,
        String status,
        List<OperationResponse> operations) {

    public static WorkOrderResponse from(WorkOrder workOrder, List<OperationResponse> operations) {
        return new WorkOrderResponse(
                workOrder.id(),
                workOrder.rocket().id(),
                workOrder.partNumber(),
                workOrder.title(),
                workOrder.workType().name(),
                workOrder.priority().name(),
                workOrder.dueAt().toString(),
                workOrder.status().name(),
                operations);
    }
}
