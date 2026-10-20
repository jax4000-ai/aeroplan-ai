package com.aeroplan.scheduling.api;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.scheduling.domain.ScheduleVersion;
import com.aeroplan.scheduling.service.PersistedCandidate;
import com.aeroplan.scheduling.service.SchedulingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final SchedulingService schedulingService;

    public ScheduleController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping("/current")
    public ScheduleVersionResponse current() {
        ScheduleVersion version = schedulingService.currentVersion();
        return ScheduleVersionResponse.from(version, schedulingService.assignmentsFor(version.id()));
    }

    @GetMapping("/{versionId}")
    public ScheduleVersionResponse getVersion(@PathVariable Long versionId) {
        ScheduleVersion version = schedulingService.getVersion(versionId);
        return ScheduleVersionResponse.from(version, schedulingService.assignmentsFor(versionId));
    }

    @PostMapping("/optimize")
    public ResponseEntity<List<CandidateResponse>> optimize(@Valid @RequestBody OptimizeRequest request) {
        Set<Long> unavailable = request.unavailableResourceIds() == null ? Set.of() : Set.copyOf(request.unavailableResourceIds());
        Instant unavailableUntil = request.unavailableUntil();
        List<PersistedCandidate> candidates = schedulingService.optimize(unavailable, unavailableUntil);
        return ResponseEntity.ok(candidates.stream().map(CandidateResponse::from).toList());
    }
}
