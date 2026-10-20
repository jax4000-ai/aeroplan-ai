package com.aeroplan.workorder.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.aeroplan.workorder.domain.AvailabilityStatus;
import com.aeroplan.workorder.domain.Dependency;
import com.aeroplan.workorder.domain.DependencyType;
import com.aeroplan.workorder.domain.Material;
import com.aeroplan.workorder.domain.MaterialStatus;
import com.aeroplan.workorder.domain.Operation;
import com.aeroplan.workorder.domain.OperationStatus;
import com.aeroplan.workorder.domain.Priority;
import com.aeroplan.workorder.domain.Resource;
import com.aeroplan.workorder.domain.Rocket;
import com.aeroplan.workorder.domain.RocketStatus;
import com.aeroplan.workorder.domain.WorkOrder;
import com.aeroplan.workorder.domain.WorkOrderStatus;
import com.aeroplan.workorder.domain.WorkType;
import com.aeroplan.workorder.repository.DependencyRepository;
import com.aeroplan.workorder.repository.MaterialRepository;
import com.aeroplan.workorder.repository.ResourceRepository;
import com.aeroplan.workorder.repository.RocketRepository;
import com.aeroplan.workorder.repository.WorkOrderRepository;

/**
 * Seeds the WO-101..105 demo scenario described in docs/architecture.md (an engine-station
 * downtime disrupting a rocket's critical path) so reviewers can immediately see a realistic
 * schedule without manually creating data. Gated behind the `demo` Spring profile — never runs
 * in `prod`.
 */
@Configuration
@Profile("demo")
public class DemoDataConfiguration {

    @Bean
    CommandLineRunner seedDemoData(
            RocketRepository rockets,
            WorkOrderRepository workOrders,
            DependencyRepository dependencies,
            ResourceRepository resources,
            MaterialRepository materials,
            Clock clock) {
        return args -> {
            if (rockets.count() > 0) return;

            Instant now = clock.instant();

            Rocket ng4 = rockets.save(new Rocket(
                    "New Glenn", "NG-4", "7-engine first stage, single BE-3U second stage",
                    now.plus(Duration.ofHours(36)), now.plus(Duration.ofHours(48)), RocketStatus.INTEGRATION));
            Rocket ng5 = rockets.save(new Rocket(
                    "New Glenn", "NG-5", "7-engine first stage, single BE-3U second stage",
                    now.plus(Duration.ofHours(400)), now.plus(Duration.ofHours(420)), RocketStatus.IN_PRODUCTION));

            resources.save(new Resource("Engine Station 01", "ASSEMBLY_STATION", List.of("MACHINIST"), 1, AvailabilityStatus.DOWN));
            resources.save(new Resource("Turbopump Station", "ASSEMBLY_STATION", List.of("ASSEMBLY_TECH"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Valve Test Stand", "TEST_STAND", List.of("QUALITY_INSPECTOR"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Integration Clean Room", "CLEAN_ROOM", List.of("INTEGRATION_TECH"), 1, AvailabilityStatus.AVAILABLE));
            resources.save(new Resource("Final QA Bay", "CLEAN_ROOM", List.of("QUALITY_INSPECTOR"), 1, AvailabilityStatus.AVAILABLE));

            materials.save(new Material("MAT-ENG-HSG", "Engine housing forging", 3, 2, "Titan Forge Co.", now.plus(Duration.ofHours(72)), MaterialStatus.IN_STOCK));
            materials.save(new Material("MAT-TURBO-SEAL", "Turbopump seal kit", 1, 4, "Precision Seals LLC", now.plus(Duration.ofHours(24)), MaterialStatus.LOW));
            materials.save(new Material("MAT-VALVE-ASSY", "Valve assembly, flight-rated", 6, 0, "AeroValve Inc.", null, MaterialStatus.IN_STOCK));

            WorkOrder wo101 = new WorkOrder(ng4, "WO-101", "Engine housing", WorkType.MANUFACTURING, Priority.CRITICAL, now.plus(Duration.ofHours(30)), WorkOrderStatus.IN_PROGRESS, now);
            Operation op101 = new Operation("Engine housing machining", 240, "MACHINIST", "ASSEMBLY_STATION", 0, OperationStatus.IN_PROGRESS);
            wo101.addOperation(op101);
            workOrders.save(wo101);

            WorkOrder wo102 = new WorkOrder(ng4, "WO-102", "Turbopump assembly", WorkType.ASSEMBLY, Priority.HIGH, now.plus(Duration.ofHours(34)), WorkOrderStatus.PLANNED, now);
            Operation op102 = new Operation("Turbopump assembly", 360, "ASSEMBLY_TECH", "ASSEMBLY_STATION", 0, OperationStatus.PLANNED);
            wo102.addOperation(op102);
            workOrders.save(wo102);

            WorkOrder wo103 = new WorkOrder(ng4, "WO-103", "Valve inspection", WorkType.TEST, Priority.HIGH, now.plus(Duration.ofHours(26)), WorkOrderStatus.PLANNED, now);
            Operation op103 = new Operation("Valve inspection", 120, "QUALITY_INSPECTOR", "TEST_STAND", 0, OperationStatus.PLANNED);
            wo103.addOperation(op103);
            workOrders.save(wo103);

            WorkOrder wo104 = new WorkOrder(ng4, "WO-104", "Engine integration", WorkType.INTEGRATION, Priority.CRITICAL, now.plus(Duration.ofHours(38)), WorkOrderStatus.BLOCKED, now);
            Operation op104 = new Operation("Engine integration", 300, "INTEGRATION_TECH", "CLEAN_ROOM", 0, OperationStatus.BLOCKED);
            wo104.addOperation(op104);
            workOrders.save(wo104);

            WorkOrder wo105 = new WorkOrder(ng4, "WO-105", "Final quality inspection", WorkType.TEST, Priority.CRITICAL, now.plus(Duration.ofHours(42)), WorkOrderStatus.PLANNED, now);
            Operation op105 = new Operation("Final quality inspection", 180, "QUALITY_INSPECTOR", "CLEAN_ROOM", 0, OperationStatus.PLANNED);
            wo105.addOperation(op105);
            workOrders.save(wo105);

            // WO-104 depends on WO-102 and WO-103; WO-105 depends on WO-104.
            dependencies.save(new Dependency(op104, op102, DependencyType.FINISH_START));
            dependencies.save(new Dependency(op104, op103, DependencyType.FINISH_START));
            dependencies.save(new Dependency(op105, op104, DependencyType.FINISH_START));

            WorkOrder wo201 = new WorkOrder(ng5, "WO-201", "Stage 1 tank weld", WorkType.MANUFACTURING, Priority.MEDIUM, now.plus(Duration.ofHours(380)), WorkOrderStatus.PLANNED, now);
            Operation op201 = new Operation("Tank weld", 480, "MACHINIST", "ASSEMBLY_STATION", 0, OperationStatus.PLANNED);
            wo201.addOperation(op201);
            workOrders.save(wo201);
        };
    }
}
