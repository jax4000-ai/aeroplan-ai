package com.aeroplan.workorder.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeroplan.workorder.api.CreateWorkOrderRequest;
import com.aeroplan.workorder.api.CreateWorkOrderRequest.OperationRequest;
import com.aeroplan.workorder.api.OperationResponse;
import com.aeroplan.workorder.api.UpdateWorkOrderRequest;
import com.aeroplan.workorder.api.WorkOrderResponse;
import com.aeroplan.workorder.common.CyclicDependencyException;
import com.aeroplan.workorder.common.NotFoundException;
import com.aeroplan.workorder.domain.Dependency;
import com.aeroplan.workorder.domain.DependencyType;
import com.aeroplan.workorder.domain.Operation;
import com.aeroplan.workorder.domain.OperationStatus;
import com.aeroplan.workorder.domain.Rocket;
import com.aeroplan.workorder.domain.WorkOrder;
import com.aeroplan.workorder.domain.WorkOrderStatus;
import com.aeroplan.workorder.repository.DependencyRepository;
import com.aeroplan.workorder.repository.RocketRepository;
import com.aeroplan.workorder.repository.WorkOrderRepository;

@Service
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final RocketRepository rocketRepository;
    private final DependencyRepository dependencyRepository;
    private final AuditService auditService;
    private final Clock clock;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            RocketRepository rocketRepository,
            DependencyRepository dependencyRepository,
            AuditService auditService,
            Clock clock) {
        this.workOrderRepository = workOrderRepository;
        this.rocketRepository = rocketRepository;
        this.dependencyRepository = dependencyRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    public List<WorkOrder> list(Long rocketId) {
        return rocketId == null ? workOrderRepository.findAll() : workOrderRepository.findAllByRocket_Id(rocketId);
    }

    public WorkOrder get(Long id) {
        return workOrderRepository.findById(id).orElseThrow(() -> NotFoundException.workOrder(id));
    }

    @Transactional
    public WorkOrder create(CreateWorkOrderRequest request, String actor) {
        Rocket rocket = rocketRepository.findById(request.rocketId()).orElseThrow(() -> NotFoundException.rocket(request.rocketId()));

        validateNoCycle(request.operations());

        WorkOrder workOrder = new WorkOrder(
                rocket, request.partNumber(), request.title(), request.workType(), request.priority(),
                request.dueAt(), WorkOrderStatus.PLANNED, clock.instant());

        Map<Integer, Operation> bySequence = new HashMap<>();
        List<OperationRequest> operationRequests = request.operations() == null ? List.of() : request.operations();
        for (OperationRequest opReq : operationRequests) {
            Operation operation = new Operation(
                    opReq.name(), opReq.durationMinutes(), opReq.requiredSkill(), opReq.requiredResourceType(),
                    opReq.sequenceIndex(), OperationStatus.PLANNED);
            workOrder.addOperation(operation);
            bySequence.put(opReq.sequenceIndex(), operation);
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        for (OperationRequest opReq : operationRequests) {
            if (opReq.dependsOnSequenceIndex() != null) {
                Operation operation = bySequence.get(opReq.sequenceIndex());
                Operation predecessor = bySequence.get(opReq.dependsOnSequenceIndex());
                dependencyRepository.save(new Dependency(operation, predecessor, DependencyType.FINISH_START));
            }
        }

        auditService.record("WORK_ORDER_CREATED", "WorkOrder", saved.id(), actor, Map.of("title", saved.title(), "rocketId", rocket.id()));
        return saved;
    }

    @Transactional
    public WorkOrder update(Long id, UpdateWorkOrderRequest request, String actor) {
        WorkOrder workOrder = get(id);
        workOrder.updateStatus(request.status(), clock.instant());
        auditService.record("WORK_ORDER_UPDATED", "WorkOrder", id, actor, Map.of("status", request.status().name()));
        return workOrder;
    }

    /**
     * Rejects a batch of new operations if any depends-on edge among them forms a cycle.
     * Dependencies in this demo are only established at creation time (see
     * UpdateWorkOrderRequest's narrower scope), so this check only ever needs to consider the
     * operations submitted together in one request.
     */
    private void validateNoCycle(List<OperationRequest> operations) {
        if (operations == null || operations.isEmpty()) return;

        Map<Integer, List<Integer>> edges = new HashMap<>();
        for (OperationRequest op : operations) {
            edges.putIfAbsent(op.sequenceIndex(), new ArrayList<>());
            if (op.dependsOnSequenceIndex() != null) {
                edges.computeIfAbsent(op.sequenceIndex(), k -> new ArrayList<>()).add(op.dependsOnSequenceIndex());
            }
        }

        Set<Integer> visited = new HashSet<>();
        Set<Integer> inStack = new HashSet<>();
        for (Integer node : edges.keySet()) {
            if (hasCycle(node, edges, visited, inStack)) {
                throw new CyclicDependencyException(
                        "Operation dependency graph contains a cycle involving sequence index " + node);
            }
        }
    }

    private boolean hasCycle(Integer node, Map<Integer, List<Integer>> edges, Set<Integer> visited, Set<Integer> inStack) {
        if (inStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        for (Integer dependsOn : edges.getOrDefault(node, List.of())) {
            if (hasCycle(dependsOn, edges, visited, inStack)) return true;
        }
        inStack.remove(node);
        return false;
    }

    public List<OperationResponse> toOperationResponses(WorkOrder workOrder) {
        List<Dependency> allDependencies = dependencyRepository.findAll();
        List<OperationResponse> result = new ArrayList<>();
        for (Operation op : workOrder.operations()) {
            List<Long> dependsOn = allDependencies.stream()
                    .filter(d -> d.operation().id().equals(op.id()))
                    .map(d -> d.predecessorOperation().id())
                    .toList();
            result.add(OperationResponse.from(op, dependsOn));
        }
        return result.stream().sorted(Comparator.comparing(r -> r.sequenceIndex())).toList();
    }

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
        return WorkOrderResponse.from(workOrder, toOperationResponses(workOrder));
    }
}
