package com.aeroplan.workorder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aeroplan.workorder.api.CreateWorkOrderRequest;
import com.aeroplan.workorder.api.CreateWorkOrderRequest.OperationRequest;
import com.aeroplan.workorder.api.RocketResponse;
import com.aeroplan.workorder.api.WorkOrderResponse;
import com.aeroplan.workorder.domain.Priority;
import com.aeroplan.workorder.domain.WorkType;
import com.aeroplan.workorder.repository.RocketRepository;
import com.aeroplan.workorder.repository.WorkOrderRepository;
import com.aeroplan.workorder.domain.Rocket;
import com.aeroplan.workorder.domain.RocketStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WorkOrderApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate client;

    @Autowired
    private RocketRepository rocketRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    private Long rocketId;

    @BeforeEach
    void setUp() {
        workOrderRepository.deleteAll();
        rocketRepository.deleteAll();
        Rocket rocket = rocketRepository.save(new Rocket("New Glenn", "NG-TEST", "test config", null, null, RocketStatus.INTEGRATION));
        rocketId = rocket.id();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void createsAndReadsAWorkOrderWithOperationsAndDependency() {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                rocketId, "WO-900", "Test work order", WorkType.MANUFACTURING, Priority.HIGH, Instant.now().plusSeconds(3600),
                List.of(
                        new OperationRequest("First op", 60, "MACHINIST", "ASSEMBLY_STATION", 0, null),
                        new OperationRequest("Second op", 30, "ASSEMBLY_TECH", "ASSEMBLY_STATION", 1, 0)));

        ResponseEntity<WorkOrderResponse> created = client.postForEntity(url("/api/work-orders"), request, WorkOrderResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().operations()).hasSize(2);
        assertThat(created.getBody().operations().get(1).dependsOnOperationIds())
                .containsExactly(created.getBody().operations().get(0).id());

        ResponseEntity<WorkOrderResponse> fetched = client.getForEntity(
                url("/api/work-orders/" + created.getBody().id()), WorkOrderResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().title()).isEqualTo("Test work order");
    }

    @Test
    void rejectsACyclicDependencyGraph() {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                rocketId, "WO-901", "Cyclic work order", WorkType.MANUFACTURING, Priority.HIGH, Instant.now().plusSeconds(3600),
                List.of(
                        new OperationRequest("Op A", 60, "MACHINIST", "ASSEMBLY_STATION", 0, 1),
                        new OperationRequest("Op B", 30, "ASSEMBLY_TECH", "ASSEMBLY_STATION", 1, 0)));

        ResponseEntity<String> response = client.postForEntity(url("/api/work-orders"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).contains("CYCLIC_DEPENDENCY");
    }

    @Test
    void listsRocketsWithWorkOrderRollup() {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                rocketId, "WO-902", "Rollup test", WorkType.ASSEMBLY, Priority.MEDIUM, Instant.now().plusSeconds(3600), List.of());
        client.postForEntity(url("/api/work-orders"), request, WorkOrderResponse.class);

        ResponseEntity<RocketResponse> response = client.getForEntity(url("/api/rockets/" + rocketId), RocketResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().workOrderCount()).isEqualTo(1);
    }

    @Test
    void returnsNotFoundForMissingWorkOrder() {
        ResponseEntity<String> response = client.getForEntity(url("/api/work-orders/999999"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("WORK_ORDER_NOT_FOUND");
    }
}
