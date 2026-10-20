package com.aeroplan.scheduling.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.aeroplan.scheduling.domain.AvailabilityStatus;
import com.aeroplan.scheduling.domain.Dependency;
import com.aeroplan.scheduling.domain.DependencyType;
import com.aeroplan.scheduling.domain.DowntimeStatus;
import com.aeroplan.scheduling.domain.MachineDowntime;
import com.aeroplan.scheduling.domain.Operation;
import com.aeroplan.scheduling.domain.OperationStatus;
import com.aeroplan.scheduling.domain.Priority;
import com.aeroplan.scheduling.domain.Resource;
import com.aeroplan.scheduling.domain.Rocket;
import com.aeroplan.scheduling.domain.RocketStatus;
import com.aeroplan.scheduling.domain.ScheduleAssignment;
import com.aeroplan.scheduling.domain.ScheduleProposal;
import com.aeroplan.scheduling.domain.ScheduleVersion;
import com.aeroplan.scheduling.domain.ScheduleVersionStatus;
import com.aeroplan.scheduling.domain.WorkOrder;
import com.aeroplan.scheduling.domain.WorkOrderStatus;
import com.aeroplan.scheduling.domain.WorkType;
import com.aeroplan.scheduling.engine.AssignmentDraft;
import com.aeroplan.scheduling.engine.ScheduleCandidateResult;
import com.aeroplan.scheduling.engine.SchedulingEngine;
import com.aeroplan.scheduling.repository.DependencyRepository;
import com.aeroplan.scheduling.repository.MachineDowntimeRepository;
import com.aeroplan.scheduling.repository.OperationRepository;
import com.aeroplan.scheduling.repository.ResourceRepository;
import com.aeroplan.scheduling.repository.RocketRepository;
import com.aeroplan.scheduling.repository.ScheduleAssignmentRepository;
import com.aeroplan.scheduling.repository.ScheduleProposalRepository;
import com.aeroplan.scheduling.repository.ScheduleVersionRepository;
import com.aeroplan.scheduling.repository.WorkOrderRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Seeds the same WO-101..105 demo scenario as work-order-service (each service keeps its own copy
 * of the read-only rows it needs in this H2/local profile — in production, one shared Postgres
 * instance already has this data). Also seeds an initial published baseline schedule and one
 * PENDING proposal (an Engine Station 01 downtime) so the whole approve/reject flow can be
 * exercised immediately. Gated behind the `demo` Spring profile — never runs in `prod`.
 */
@Configuration
@Profile("demo")
public class DemoDataConfiguration {

    @Bean
    CommandLineRunner seedDemoData(
            RocketRepository rockets,
            WorkOrderRepository workOrders,
            OperationRepository operations,
            DependencyRepository dependencies,
            ResourceRepository resources,
            MachineDowntimeRepository machineDowntimes,
            ScheduleVersionRepository versions,
            ScheduleAssignmentRepository assignments,
            ScheduleProposalRepository proposals,
            SchedulingEngine engine,
            ObjectMapper objectMapper,
            Clock clock) {
        return args -> {
            if (rockets.count() > 0) return;

            Instant now = clock.instant();

            Rocket ng4 = rockets.save(new Rocket("New Glenn", "NG-4", "7-engine first stage, single BE-3U second stage",
                    now.plus(Duration.ofHours(36)), now.plus(Duration.ofHours(48)), RocketStatus.INTEGRATION));
            Rocket ng5 = rockets.save(new Rocket("New Glenn", "NG-5", "7-engine first stage, single BE-3U second stage",
                    now.plus(Duration.ofHours(400)), now.plus(Duration.ofHours(420)), RocketStatus.IN_PRODUCTION));

            Resource engineStation = resources.save(new Resource("Engine Station 01", "ASSEMBLY_STATION", List.of("MACHINIST"), 1, AvailabilityStatus.DOWN));
            resources.save(new Resource("Turbopump Station", "ASSEMBLY_STATION", List.of("ASSEMBLY_TECH"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Valve Test Stand", "TEST_STAND", List.of("QUALITY_INSPECTOR"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Integration Clean Room", "CLEAN_ROOM", List.of("INTEGRATION_TECH"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Final QA Bay", "CLEAN_ROOM", List.of("QUALITY_INSPECTOR"), 1, AvailabilityStatus.AVAILABLE));

            Instant recoveryAt = now.plus(Duration.ofHours(6));
            MachineDowntime downtime = machineDowntimes.save(new MachineDowntime("evt-9001", engineStation.id(), now,
                    recoveryAt, "Preventive maintenance", DowntimeStatus.OPEN));

            WorkOrder wo101 = workOrders.save(new WorkOrder(ng4.id(), "WO-101", "Engine housing", WorkType.MANUFACTURING,
                    Priority.CRITICAL, now.plus(Duration.ofHours(30)), WorkOrderStatus.IN_PROGRESS));
            Operation op101 = operations.save(new Operation(wo101.id(), "Engine housing machining", 240, "MACHINIST",
                    "ASSEMBLY_STATION", 0, OperationStatus.IN_PROGRESS));

            WorkOrder wo102 = workOrders.save(new WorkOrder(ng4.id(), "WO-102", "Turbopump assembly", WorkType.ASSEMBLY,
                    Priority.HIGH, now.plus(Duration.ofHours(34)), WorkOrderStatus.PLANNED));
            Operation op102 = operations.save(new Operation(wo102.id(), "Turbopump assembly", 360, "ASSEMBLY_TECH",
                    "ASSEMBLY_STATION", 0, OperationStatus.PLANNED));

            WorkOrder wo103 = workOrders.save(new WorkOrder(ng4.id(), "WO-103", "Valve inspection", WorkType.TEST,
                    Priority.HIGH, now.plus(Duration.ofHours(26)), WorkOrderStatus.PLANNED));
            Operation op103 = operations.save(new Operation(wo103.id(), "Valve inspection", 120, "QUALITY_INSPECTOR",
                    "TEST_STAND", 0, OperationStatus.PLANNED));

            WorkOrder wo104 = workOrders.save(new WorkOrder(ng4.id(), "WO-104", "Engine integration", WorkType.INTEGRATION,
                    Priority.CRITICAL, now.plus(Duration.ofHours(38)), WorkOrderStatus.BLOCKED));
            Operation op104 = operations.save(new Operation(wo104.id(), "Engine integration", 300, "INTEGRATION_TECH",
                    "CLEAN_ROOM", 0, OperationStatus.BLOCKED));

            WorkOrder wo105 = workOrders.save(new WorkOrder(ng4.id(), "WO-105", "Final quality inspection", WorkType.TEST,
                    Priority.CRITICAL, now.plus(Duration.ofHours(42)), WorkOrderStatus.PLANNED));
            Operation op105 = operations.save(new Operation(wo105.id(), "Final quality inspection", 180, "QUALITY_INSPECTOR",
                    "CLEAN_ROOM", 0, OperationStatus.PLANNED));

            dependencies.save(new Dependency(op104.id(), op102.id(), DependencyType.FINISH_START));
            dependencies.save(new Dependency(op104.id(), op103.id(), DependencyType.FINISH_START));
            dependencies.save(new Dependency(op105.id(), op104.id(), DependencyType.FINISH_START));

            WorkOrder wo201 = workOrders.save(new WorkOrder(ng5.id(), "WO-201", "Stage 1 tank weld", WorkType.MANUFACTURING,
                    Priority.MEDIUM, now.plus(Duration.ofHours(380)), WorkOrderStatus.PLANNED));
            operations.save(new Operation(wo201.id(), "Tank weld", 480, "MACHINIST", "ASSEMBLY_STATION", 0, OperationStatus.PLANNED));

            // Baseline published schedule (version 1) — computed with no disruption so /api/schedules/current works immediately.
            List<WorkOrder> allWorkOrders = workOrders.findAll();
            List<Operation> allOperations = operations.findAll();
            List<Dependency> allDependencies = dependencies.findAll();
            List<Resource> allResources = resources.findAll();

            ScheduleCandidateResult baselineResult = engine.generateCandidates(allWorkOrders, allOperations, allDependencies,
                    allResources, Set.of(), null, now, Map.of()).get(0);
            ScheduleVersion baselineVersion = versions.save(new ScheduleVersion(1, ScheduleVersionStatus.PUBLISHED, baselineResult.objectiveScore()));
            baselineVersion.current(true);
            versions.save(baselineVersion);
            Map<Long, AssignmentDraft> baselineByOperation = new HashMap<>();
            for (AssignmentDraft draft : baselineResult.assignments()) {
                if (draft.resourceId() == null) continue;
                assignments.save(new ScheduleAssignment(baselineVersion, draft.operationId(), draft.resourceId(), draft.startAt(), draft.endAt()));
                baselineByOperation.put(draft.operationId(), draft);
            }

            // Disruption candidate (version 2) — Engine Station 01 down until `recoveryAt`.
            ScheduleCandidateResult candidateResult = engine.generateCandidates(allWorkOrders, allOperations, allDependencies,
                    allResources, Set.of(engineStation.id()), recoveryAt, now, baselineByOperation).get(0);
            ScheduleVersion candidateVersion = versions.save(new ScheduleVersion(2, ScheduleVersionStatus.PROPOSED, candidateResult.objectiveScore()));
            for (AssignmentDraft draft : candidateResult.assignments()) {
                if (draft.resourceId() == null) continue;
                assignments.save(new ScheduleAssignment(candidateVersion, draft.operationId(), draft.resourceId(), draft.startAt(), draft.endAt()));
            }

            Map<String, Object> agentFindings = new HashMap<>();
            agentFindings.put("schedulingOptimization", Map.of("candidates", List.of(
                    Map.of("scheduleVersionId", candidateVersion.id(), "objectiveScore", candidateResult.objectiveScore(),
                            "overtimeMinutesUsed", candidateResult.overtimeMinutesUsed(),
                            "changedAssignments", candidateResult.changedAssignments()))));
            agentFindings.put("materialsReadiness", Map.of("blockedOperations", List.of(), "availableNow", List.of(op102.id(), op103.id())));
            agentFindings.put("bottleneckRisk", Map.of("atRiskWorkOrders",
                    candidateResult.lateWorkOrderHours().isEmpty() ? List.of()
                            : List.of(Map.of("workOrderId", wo104.id(), "lateProbability", 0.7))));
            agentFindings.put("launchReadiness", Map.of("affectedRocketIds", List.of(ng4.id()), "readinessImpact", "NONE"));
            agentFindings.put("recovery", Map.of("recommendedCandidateIndex", 0,
                    "tradeoffs", "Protects WO-101 and WO-104 critical deadlines at the cost of schedule churn while Engine Station 01 is down."));

            String findingsJson;
            try {
                findingsJson = objectMapper.writeValueAsString(agentFindings);
            } catch (Exception e) {
                findingsJson = "{}";
            }

            proposals.save(new ScheduleProposal(downtime.id(), baselineVersion.id(), candidateVersion.id(),
                    "Recommend Candidate A: protects WO-101 and WO-104 critical deadlines while Engine Station 01 recovers from preventive maintenance.",
                    findingsJson, "demo-task-token", "arn:aws:states:us-east-2:420374519668:execution:disruption-recovery:demo-exec-1"));
        };
    }
}
