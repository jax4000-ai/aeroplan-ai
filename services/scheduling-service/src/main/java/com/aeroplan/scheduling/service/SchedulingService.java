package com.aeroplan.scheduling.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeroplan.scheduling.common.ErrorCode;
import com.aeroplan.scheduling.common.NotFoundException;
import com.aeroplan.scheduling.domain.Dependency;
import com.aeroplan.scheduling.domain.Operation;
import com.aeroplan.scheduling.domain.Resource;
import com.aeroplan.scheduling.domain.ScheduleAssignment;
import com.aeroplan.scheduling.domain.ScheduleVersion;
import com.aeroplan.scheduling.domain.ScheduleVersionStatus;
import com.aeroplan.scheduling.domain.WorkOrder;
import com.aeroplan.scheduling.engine.AssignmentDraft;
import com.aeroplan.scheduling.engine.ScheduleCandidateResult;
import com.aeroplan.scheduling.engine.SchedulingEngine;
import com.aeroplan.scheduling.repository.DependencyRepository;
import com.aeroplan.scheduling.repository.OperationRepository;
import com.aeroplan.scheduling.repository.ResourceRepository;
import com.aeroplan.scheduling.repository.ScheduleAssignmentRepository;
import com.aeroplan.scheduling.repository.ScheduleVersionRepository;
import com.aeroplan.scheduling.repository.WorkOrderRepository;

/** Orchestrates the deterministic scheduling engine against the shared read-only domain data. */
@Service
public class SchedulingService {
    private final WorkOrderRepository workOrderRepository;
    private final OperationRepository operationRepository;
    private final DependencyRepository dependencyRepository;
    private final ResourceRepository resourceRepository;
    private final ScheduleVersionRepository versionRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final SchedulingEngine engine;
    private final Clock clock;

    public SchedulingService(WorkOrderRepository workOrderRepository, OperationRepository operationRepository,
            DependencyRepository dependencyRepository, ResourceRepository resourceRepository,
            ScheduleVersionRepository versionRepository, ScheduleAssignmentRepository assignmentRepository,
            SchedulingEngine engine, Clock clock) {
        this.workOrderRepository = workOrderRepository;
        this.operationRepository = operationRepository;
        this.dependencyRepository = dependencyRepository;
        this.resourceRepository = resourceRepository;
        this.versionRepository = versionRepository;
        this.assignmentRepository = assignmentRepository;
        this.engine = engine;
        this.clock = clock;
    }

    public ScheduleVersion currentVersion() {
        return versionRepository.findByCurrentTrue()
                .orElseThrow(() -> new NotFoundException(ErrorCode.SCHEDULE_VERSION_NOT_FOUND,
                        "No published schedule version exists yet"));
    }

    public ScheduleVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SCHEDULE_VERSION_NOT_FOUND,
                        "Schedule version " + versionId + " was not found"));
    }

    public List<ScheduleAssignment> assignmentsFor(Long versionId) {
        return assignmentRepository.findByScheduleVersionId(versionId);
    }

    /**
     * Publishes a DRAFT/PROPOSED candidate as the new current schedule, superseding whichever
     * version was previously current. Called by {@code ProposalService} on approval.
     */
    @Transactional
    public void publish(Long candidateVersionId) {
        ScheduleVersion candidate = getVersion(candidateVersionId);
        versionRepository.findByCurrentTrue().ifPresent(previous -> {
            previous.current(false);
            previous.status(ScheduleVersionStatus.SUPERSEDED);
            versionRepository.save(previous);
        });
        candidate.status(ScheduleVersionStatus.PUBLISHED);
        candidate.current(true);
        versionRepository.save(candidate);
    }

    /** Called when a planner rejects the proposal built on top of this candidate. */
    @Transactional
    public void markRejected(Long candidateVersionId) {
        if (candidateVersionId == null) {
            return;
        }
        ScheduleVersion candidate = getVersion(candidateVersionId);
        candidate.status(ScheduleVersionStatus.REJECTED);
        versionRepository.save(candidate);
    }

    /**
     * Runs the engine and persists each candidate as a {@code DRAFT} schedule version — draft
     * versions are never published automatically; only {@code ProposalService.approve} promotes
     * one to {@code PUBLISHED}/current.
     */
    @Transactional
    public List<PersistedCandidate> optimize(Set<Long> unavailableResourceIds, Instant unavailableUntil) {
        List<WorkOrder> workOrders = workOrderRepository.findAll();
        List<Long> workOrderIds = workOrders.stream().map(WorkOrder::id).toList();
        List<Operation> operations = operationRepository.findByWorkOrderIdIn(workOrderIds);
        List<Dependency> dependencies = dependencyRepository.findAll();
        List<Resource> resources = resourceRepository.findAll();

        Map<Long, AssignmentDraft> baseline = new HashMap<>();
        versionRepository.findByCurrentTrue().ifPresent(current -> {
            for (ScheduleAssignment a : assignmentRepository.findByScheduleVersionId(current.id())) {
                baseline.put(a.operationId(), new AssignmentDraft(a.operationId(), a.resourceId(), a.startAt(), a.endAt()));
            }
        });

        List<ScheduleCandidateResult> candidates = engine.generateCandidates(workOrders, operations, dependencies,
                resources, unavailableResourceIds == null ? Set.of() : unavailableResourceIds, unavailableUntil,
                clock.instant(), baseline);

        List<PersistedCandidate> persisted = new ArrayList<>();
        for (ScheduleCandidateResult candidate : candidates) {
            ScheduleVersion version = persistDraft(candidate);
            persisted.add(new PersistedCandidate(version.id(), candidate));
        }
        return persisted;
    }

    private ScheduleVersion persistDraft(ScheduleCandidateResult candidate) {
        int nextVersionNumber = versionRepository.findTopByOrderByVersionNumberDesc()
                .map(v -> v.versionNumber() + 1)
                .orElse(1);
        ScheduleVersion version = versionRepository.save(
                new ScheduleVersion(nextVersionNumber, ScheduleVersionStatus.DRAFT, candidate.objectiveScore()));
        for (AssignmentDraft draft : candidate.assignments()) {
            if (draft.resourceId() == null) {
                continue; // no feasible resource of the required type — surfaced via lateWorkOrderHours instead
            }
            assignmentRepository.save(new ScheduleAssignment(version, draft.operationId(), draft.resourceId(),
                    draft.startAt(), draft.endAt()));
        }
        return version;
    }
}
