package com.aeroplan.scheduling.engine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aeroplan.scheduling.domain.AvailabilityStatus;
import com.aeroplan.scheduling.domain.Dependency;
import com.aeroplan.scheduling.domain.Operation;
import com.aeroplan.scheduling.domain.Priority;
import com.aeroplan.scheduling.domain.Resource;
import com.aeroplan.scheduling.domain.WorkOrder;

/**
 * Deterministic list-scheduling engine (ADR: the engine is the source of truth for feasibility;
 * agents only read its output, never invent their own schedule — see ADR-003).
 *
 * <p>Known simplifications (documented rather than hidden): {@code FINISH_FINISH} dependencies are
 * treated identically to {@code FINISH_START} (predecessor must complete before the successor can
 * start); all work happens in UTC against one global "standard hours" window
 * ({@value #DEFAULT_STANDARD_HOURS_NOTE}) rather than per-facility calendars/timezones.
 */
@Service
public class SchedulingEngine {
    private static final String DEFAULT_STANDARD_HOURS_NOTE = "07:00-19:00 UTC, every day";
    private static final Duration UNAVAILABLE_FALLBACK_HORIZON = Duration.ofDays(30);

    private final double lateHourWeight;
    private final double overtimeHourWeight;
    private final double churnWeight;
    private final int standardHourStart;
    private final int standardHourEnd;

    public SchedulingEngine(
            @Value("${scheduling.engine.weight.late-hours:10.0}") double lateHourWeight,
            @Value("${scheduling.engine.weight.overtime-hours:4.0}") double overtimeHourWeight,
            @Value("${scheduling.engine.weight.changed-assignments:1.0}") double churnWeight,
            @Value("${scheduling.engine.standard-hours.start:7}") int standardHourStart,
            @Value("${scheduling.engine.standard-hours.end:19}") int standardHourEnd) {
        this.lateHourWeight = lateHourWeight;
        this.overtimeHourWeight = overtimeHourWeight;
        this.churnWeight = churnWeight;
        this.standardHourStart = standardHourStart;
        this.standardHourEnd = standardHourEnd;
    }

    /**
     * Produces two candidate plans using different tie-break strategies among "ready" operations
     * (all predecessors complete): {@code priority-first} protects CRITICAL/HIGH work first;
     * {@code deadline-first} always schedules whatever is due soonest first. Both respect the same
     * dependency graph and resource capacity/availability constraints.
     */
    public List<ScheduleCandidateResult> generateCandidates(
            List<WorkOrder> workOrders,
            List<Operation> operations,
            List<Dependency> dependencies,
            List<Resource> resources,
            Set<Long> unavailableResourceIds,
            Instant unavailableUntil,
            Instant planningStart,
            Map<Long, AssignmentDraft> previousAssignments) {
        Map<Long, WorkOrder> workOrderById = new HashMap<>();
        for (WorkOrder wo : workOrders) {
            workOrderById.put(wo.id(), wo);
        }

        Comparator<Operation> priorityFirst = comparator(workOrderById, true);
        Comparator<Operation> deadlineFirst = comparator(workOrderById, false);

        ScheduleCandidateResult candidateA = runOnce("Priority-first (protects CRITICAL/HIGH deadlines)", workOrderById,
                operations, dependencies, resources, unavailableResourceIds, unavailableUntil, planningStart,
                previousAssignments, priorityFirst);
        ScheduleCandidateResult candidateB = runOnce("Deadline-first (strict earliest-due-date order)", workOrderById,
                operations, dependencies, resources, unavailableResourceIds, unavailableUntil, planningStart,
                previousAssignments, deadlineFirst);

        List<ScheduleCandidateResult> results = new ArrayList<>();
        results.add(candidateA);
        // Only surface the second candidate if it actually produces a different plan — no point
        // showing a planner two identical options.
        if (!candidateA.assignments().equals(candidateB.assignments())) {
            results.add(candidateB);
        }
        return results;
    }

    private Comparator<Operation> comparator(Map<Long, WorkOrder> workOrderById, boolean priorityBeforeDeadline) {
        Comparator<Operation> byPriority = Comparator.comparingInt(op -> priorityRank(workOrderById.get(op.workOrderId())));
        Comparator<Operation> byDueAt = Comparator.comparing(op -> workOrderById.get(op.workOrderId()).dueAt());
        Comparator<Operation> primary = priorityBeforeDeadline ? byPriority.thenComparing(byDueAt) : byDueAt.thenComparing(byPriority);
        return primary.thenComparingInt(Operation::sequenceIndex).thenComparing(Operation::id);
    }

    private int priorityRank(WorkOrder workOrder) {
        if (workOrder == null || workOrder.priority() == null) {
            return Priority.LOW.ordinal();
        }
        return workOrder.priority().ordinal();
    }

    private ScheduleCandidateResult runOnce(
            String label,
            Map<Long, WorkOrder> workOrderById,
            List<Operation> operations,
            List<Dependency> dependencies,
            List<Resource> resources,
            Set<Long> unavailableResourceIds,
            Instant unavailableUntil,
            Instant planningStart,
            Map<Long, AssignmentDraft> previousAssignments,
            Comparator<Operation> readyOrder) {
        Map<Long, Operation> operationById = new HashMap<>();
        for (Operation op : operations) {
            operationById.put(op.id(), op);
        }

        Map<Long, List<Long>> predecessorsOf = new HashMap<>();
        Map<Long, List<Long>> successorsOf = new HashMap<>();
        for (Operation op : operations) {
            predecessorsOf.put(op.id(), new ArrayList<>());
            successorsOf.put(op.id(), new ArrayList<>());
        }
        for (Dependency dep : dependencies) {
            predecessorsOf.computeIfAbsent(dep.operationId(), k -> new ArrayList<>()).add(dep.predecessorOperationId());
            successorsOf.computeIfAbsent(dep.predecessorOperationId(), k -> new ArrayList<>()).add(dep.operationId());
        }

        Map<Long, Integer> inDegree = new HashMap<>();
        for (Operation op : operations) {
            inDegree.put(op.id(), predecessorsOf.getOrDefault(op.id(), List.of()).size());
        }

        TreeSet<Operation> ready = new TreeSet<>(readyOrder);
        for (Operation op : operations) {
            if (inDegree.get(op.id()) == 0) {
                ready.add(op);
            }
        }

        Map<Long, PriorityQueue<Instant>> resourceSlots = new HashMap<>();
        for (Resource resource : resources) {
            Instant baseFreeAt = planningStart;
            boolean disrupted = unavailableResourceIds.contains(resource.id())
                    || resource.availabilityStatus() != AvailabilityStatus.AVAILABLE;
            if (disrupted) {
                baseFreeAt = unavailableUntil != null ? unavailableUntil : planningStart.plus(UNAVAILABLE_FALLBACK_HORIZON);
            }
            PriorityQueue<Instant> slots = new PriorityQueue<>();
            for (int i = 0; i < Math.max(1, resource.capacity()); i++) {
                slots.add(baseFreeAt);
            }
            resourceSlots.put(resource.id(), slots);
        }

        Map<Long, Instant> operationEndTime = new HashMap<>();
        List<AssignmentDraft> assignments = new ArrayList<>();
        Map<Long, Instant> workOrderMaxEnd = new HashMap<>();
        long overtimeMinutes = 0;
        int changedAssignments = 0;

        Set<Long> scheduled = new HashSet<>();
        while (!ready.isEmpty()) {
            Operation op = ready.pollFirst();
            scheduled.add(op.id());

            Instant earliestStart = planningStart;
            for (Long predId : predecessorsOf.getOrDefault(op.id(), List.of())) {
                Instant predEnd = operationEndTime.get(predId);
                if (predEnd != null && predEnd.isAfter(earliestStart)) {
                    earliestStart = predEnd;
                }
            }

            List<Resource> candidates = resources.stream()
                    .filter(r -> r.resourceType().equals(op.requiredResourceType()))
                    .filter(r -> r.skills().isEmpty() || r.skills().contains(op.requiredSkill()))
                    .toList();
            if (candidates.isEmpty()) {
                // No resource of the required type exists at all — a data problem upstream. Fall
                // back to scheduling it with no resource contention so the engine still terminates.
                Instant start = earliestStart;
                Instant end = start.plus(Duration.ofMinutes(op.durationMinutes()));
                assignments.add(new AssignmentDraft(op.id(), null, start, end));
                operationEndTime.put(op.id(), end);
            } else {
                Resource chosen = null;
                Instant chosenStart = null;
                for (Resource candidate : candidates) {
                    Instant slotFree = resourceSlots.get(candidate.id()).peek();
                    Instant candidateStart = slotFree.isAfter(earliestStart) ? slotFree : earliestStart;
                    if (chosenStart == null || candidateStart.isBefore(chosenStart)
                            || (candidateStart.equals(chosenStart) && candidate.id() < chosen.id())) {
                        chosen = candidate;
                        chosenStart = candidateStart;
                    }
                }
                resourceSlots.get(chosen.id()).poll();
                Instant end = chosenStart.plus(Duration.ofMinutes(op.durationMinutes()));
                resourceSlots.get(chosen.id()).add(end);
                assignments.add(new AssignmentDraft(op.id(), chosen.id(), chosenStart, end));
                operationEndTime.put(op.id(), end);
                overtimeMinutes += minutesOutsideStandardHours(chosenStart, end);
            }

            Instant assignedEnd = operationEndTime.get(op.id());
            Long workOrderId = op.workOrderId();
            Instant currentMax = workOrderMaxEnd.get(workOrderId);
            if (currentMax == null || assignedEnd.isAfter(currentMax)) {
                workOrderMaxEnd.put(workOrderId, assignedEnd);
            }

            AssignmentDraft draft = assignments.get(assignments.size() - 1);
            AssignmentDraft previous = previousAssignments == null ? null : previousAssignments.get(op.id());
            if (previous == null || !previous.resourceId().equals(draft.resourceId())
                    || Duration.between(previous.startAt(), draft.startAt()).abs().toMinutes() > 1) {
                changedAssignments++;
            }

            for (Long successorId : successorsOf.getOrDefault(op.id(), List.of())) {
                int remaining = inDegree.merge(successorId, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(operationById.get(successorId));
                }
            }
        }

        // Defensive fallback: any operation never reached (e.g. a dependency graph anomaly that
        // slipped past work-order-service's own cycle rejection) is appended at the end rather
        // than silently dropped, so the plan always covers every operation.
        for (Operation op : operations) {
            if (!scheduled.contains(op.id())) {
                Instant start = planningStart;
                Instant end = start.plus(Duration.ofMinutes(op.durationMinutes()));
                assignments.add(new AssignmentDraft(op.id(), null, start, end));
                workOrderMaxEnd.merge(op.workOrderId(), end, (a, b) -> b.isAfter(a) ? b : a);
                changedAssignments++;
            }
        }

        Map<Long, Long> lateWorkOrderHours = new HashMap<>();
        for (WorkOrder wo : workOrderById.values()) {
            Instant maxEnd = workOrderMaxEnd.get(wo.id());
            if (maxEnd != null && maxEnd.isAfter(wo.dueAt())) {
                long lateHours = (long) Math.ceil(Duration.between(wo.dueAt(), maxEnd).toMinutes() / 60.0);
                if (lateHours > 0) {
                    lateWorkOrderHours.put(wo.id(), lateHours);
                }
            }
        }

        double objectiveScore = lateHourWeight * lateWorkOrderHours.values().stream().mapToLong(Long::longValue).sum()
                + overtimeHourWeight * (overtimeMinutes / 60.0)
                + churnWeight * changedAssignments;

        return new ScheduleCandidateResult(label, objectiveScore, lateWorkOrderHours, overtimeMinutes, changedAssignments, assignments);
    }

    private long minutesOutsideStandardHours(Instant start, Instant end) {
        long minutes = 0;
        ZonedDateTime cursor = start.atZone(ZoneOffset.UTC);
        ZonedDateTime endZoned = end.atZone(ZoneOffset.UTC);
        while (cursor.isBefore(endZoned)) {
            LocalDate day = cursor.toLocalDate();
            ZonedDateTime dayOpen = day.atStartOfDay(ZoneOffset.UTC).plusHours(standardHourStart);
            ZonedDateTime dayClose = day.atStartOfDay(ZoneOffset.UTC).plusHours(standardHourEnd);
            ZonedDateTime segmentEnd = cursor.plusDays(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime windowEnd = segmentEnd.isBefore(endZoned) ? segmentEnd : endZoned;

            ZonedDateTime insideStart = cursor.isAfter(dayOpen) ? cursor : dayOpen;
            ZonedDateTime insideEnd = windowEnd.isBefore(dayClose) ? windowEnd : dayClose;
            long insideMinutes = insideEnd.isAfter(insideStart) ? Duration.between(insideStart, insideEnd).toMinutes() : 0;
            long totalMinutes = Duration.between(cursor, windowEnd).toMinutes();
            minutes += Math.max(0, totalMinutes - insideMinutes);

            cursor = windowEnd;
        }
        return minutes;
    }
}
