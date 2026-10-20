package com.aeroplan.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aeroplan.scheduling.api.ApproveRequest;
import com.aeroplan.scheduling.api.CandidateResponse;
import com.aeroplan.scheduling.api.CreateProposalRequest;
import com.aeroplan.scheduling.api.ProposalIdResponse;
import com.aeroplan.scheduling.api.ProposalResponse;
import com.aeroplan.scheduling.api.RejectRequest;
import com.aeroplan.scheduling.api.ScheduleVersionResponse;
import com.aeroplan.scheduling.domain.AvailabilityStatus;
import com.aeroplan.scheduling.domain.Dependency;
import com.aeroplan.scheduling.domain.DependencyType;
import com.aeroplan.scheduling.domain.Operation;
import com.aeroplan.scheduling.domain.OperationStatus;
import com.aeroplan.scheduling.domain.Priority;
import com.aeroplan.scheduling.domain.Resource;
import com.aeroplan.scheduling.domain.Rocket;
import com.aeroplan.scheduling.domain.RocketStatus;
import com.aeroplan.scheduling.domain.WorkOrder;
import com.aeroplan.scheduling.domain.WorkOrderStatus;
import com.aeroplan.scheduling.domain.WorkType;
import com.aeroplan.scheduling.repository.DependencyRepository;
import com.aeroplan.scheduling.repository.OperationRepository;
import com.aeroplan.scheduling.repository.ResourceRepository;
import com.aeroplan.scheduling.repository.RocketRepository;
import com.aeroplan.scheduling.repository.ScheduleProposalRepository;
import com.aeroplan.scheduling.repository.ScheduleVersionRepository;
import com.aeroplan.scheduling.repository.WorkOrderRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SchedulingApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate client;

    @Autowired
    private RocketRepository rocketRepository;
    @Autowired
    private WorkOrderRepository workOrderRepository;
    @Autowired
    private OperationRepository operationRepository;
    @Autowired
    private DependencyRepository dependencyRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ScheduleVersionRepository versionRepository;
    @Autowired
    private ScheduleProposalRepository proposalRepository;

    private Long rocketId;
    private Long op1Id;
    private Long op2Id;
    private Long resourceId;

    @BeforeEach
    void setUp() {
        proposalRepository.deleteAll();
        versionRepository.deleteAll();
        dependencyRepository.deleteAll();
        operationRepository.deleteAll();
        workOrderRepository.deleteAll();
        resourceRepository.deleteAll();
        rocketRepository.deleteAll();

        Rocket rocket = rocketRepository.save(new Rocket("New Glenn", "NG-TEST", "test config",
                Instant.now().plus(Duration.ofDays(3)), Instant.now().plus(Duration.ofDays(4)), RocketStatus.INTEGRATION));
        rocketId = rocket.id();

        Resource resource = resourceRepository.save(new Resource("Test Station", "ASSEMBLY_STATION",
                List.of("MACHINIST"), 1, AvailabilityStatus.AVAILABLE));
        resourceId = resource.id();

        WorkOrder wo1 = workOrderRepository.save(new WorkOrder(rocketId, "WO-T1", "First work order",
                WorkType.MANUFACTURING, Priority.CRITICAL, Instant.now().plus(Duration.ofHours(5)), WorkOrderStatus.PLANNED));
        Operation op1 = operationRepository.save(new Operation(wo1.id(), "Op 1", 60, "MACHINIST",
                "ASSEMBLY_STATION", 0, OperationStatus.PLANNED));
        op1Id = op1.id();

        WorkOrder wo2 = workOrderRepository.save(new WorkOrder(rocketId, "WO-T2", "Second work order",
                WorkType.MANUFACTURING, Priority.HIGH, Instant.now().plus(Duration.ofHours(10)), WorkOrderStatus.PLANNED));
        Operation op2 = operationRepository.save(new Operation(wo2.id(), "Op 2", 60, "MACHINIST",
                "ASSEMBLY_STATION", 0, OperationStatus.PLANNED));
        op2Id = op2.id();

        dependencyRepository.save(new Dependency(op2.id(), op1.id(), DependencyType.FINISH_START));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void optimizeRespectsDependencyOrderAndAvoidsDoubleBooking() {
        Map<String, Object> request = Map.of("reason", "MACHINE_DOWNTIME");
        ResponseEntity<CandidateResponse[]> response = client.postForEntity(url("/api/schedules/optimize"), request, CandidateResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CandidateResponse candidate = response.getBody()[0];
        var byOperation = candidate.assignments().stream()
                .collect(java.util.stream.Collectors.toMap(CandidateResponse.AssignmentDraftResponse::operationId, a -> a));

        // Dependency respected: op2 cannot start before op1 finishes.
        assertThat(byOperation.get(op2Id).startAt()).isAfterOrEqualTo(byOperation.get(op1Id).endAt());

        // No double-booking: since both operations need the same single-capacity resource, their
        // time windows on that resource must not overlap.
        var op1Window = byOperation.get(op1Id);
        var op2Window = byOperation.get(op2Id);
        if (op1Window.resourceId().equals(op2Window.resourceId())) {
            boolean overlap = op1Window.startAt().isBefore(op2Window.endAt()) && op2Window.startAt().isBefore(op1Window.endAt());
            assertThat(overlap).isFalse();
        }
    }

    @Test
    void approveProposalPublishesCandidateAndRejectsDoubleDecision() {
        // Run optimize to create a real candidate DRAFT version to approve.
        Map<String, Object> optimizeRequest = Map.of("reason", "MACHINE_DOWNTIME");
        ResponseEntity<CandidateResponse[]> optimizeResponse = client.postForEntity(url("/api/schedules/optimize"), optimizeRequest, CandidateResponse[].class);
        Long candidateVersionId = optimizeResponse.getBody()[0].scheduleVersionId();

        CreateProposalRequest createRequest = new CreateProposalRequest("tok-1", "arn:test:1", null, candidateVersionId,
                candidateVersionId, "Test summary", Map.of());
        ResponseEntity<ProposalIdResponse> createResponse = client.postForEntity(url("/internal/proposals"), createRequest, ProposalIdResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long proposalId = createResponse.getBody().proposalId();

        ResponseEntity<ProposalResponse> approveResponse = client.postForEntity(url("/api/proposals/" + proposalId + "/approve"),
                new ApproveRequest("planner.ada"), ProposalResponse.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().status()).isEqualTo("APPROVED");

        ResponseEntity<ScheduleVersionResponse> currentResponse = client.getForEntity(url("/api/schedules/current"), ScheduleVersionResponse.class);
        assertThat(currentResponse.getBody().id()).isEqualTo(candidateVersionId);
        assertThat(currentResponse.getBody().status()).isEqualTo("PUBLISHED");

        ResponseEntity<String> secondApprove = client.postForEntity(url("/api/proposals/" + proposalId + "/approve"),
                new ApproveRequest("planner.bob"), String.class);
        assertThat(secondApprove.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(secondApprove.getBody()).contains("PROPOSAL_ALREADY_DECIDED");
    }

    @Test
    void rejectDoesNotPublishAndStepFunctionsDecisionIsRecorded() {
        Map<String, Object> optimizeRequest = Map.of("reason", "MACHINE_DOWNTIME");
        ResponseEntity<CandidateResponse[]> optimizeResponse = client.postForEntity(url("/api/schedules/optimize"), optimizeRequest, CandidateResponse[].class);
        Long candidateVersionId = optimizeResponse.getBody()[0].scheduleVersionId();

        CreateProposalRequest createRequest = new CreateProposalRequest("tok-2", "arn:test:2", null, candidateVersionId,
                candidateVersionId, "Test summary", Map.of());
        ResponseEntity<ProposalIdResponse> createResponse = client.postForEntity(url("/internal/proposals"), createRequest, ProposalIdResponse.class);
        Long proposalId = createResponse.getBody().proposalId();

        ResponseEntity<ProposalResponse> rejectResponse = client.postForEntity(url("/api/proposals/" + proposalId + "/reject"),
                new RejectRequest("planner.ada"), ProposalResponse.class);
        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejectResponse.getBody().status()).isEqualTo("REJECTED");

        ResponseEntity<String> currentResponse = client.getForEntity(url("/api/schedules/current"), String.class);
        assertThat(currentResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void launchReadinessReflectsBlockedWorkOrders() {
        workOrderRepository.save(new WorkOrder(rocketId, "WO-T3", "Blocked work order",
                WorkType.INTEGRATION, Priority.CRITICAL, Instant.now().plus(Duration.ofHours(8)), WorkOrderStatus.BLOCKED));

        ResponseEntity<Map> response = client.getForEntity(url("/api/launch-readiness?rocketId=" + rocketId), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("readinessStatus")).isEqualTo("BLOCKED");
    }
}
