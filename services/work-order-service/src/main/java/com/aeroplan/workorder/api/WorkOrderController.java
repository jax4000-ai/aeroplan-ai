package com.aeroplan.workorder.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.workorder.domain.WorkOrder;
import com.aeroplan.workorder.service.WorkOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public List<WorkOrderResponse> list(@RequestParam(required = false) Long rocketId) {
        return workOrderService.list(rocketId).stream().map(workOrderService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable Long id) {
        return workOrderService.toResponse(workOrderService.get(id));
    }

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(
            @Valid @RequestBody CreateWorkOrderRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "planner.ada") String actor) {
        WorkOrder created = workOrderService.create(request, actor);
        return ResponseEntity.status(201).body(workOrderService.toResponse(created));
    }

    @PutMapping("/{id}")
    public WorkOrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "planner.ada") String actor) {
        return workOrderService.toResponse(workOrderService.update(id, request, actor));
    }
}
