package com.aeroplan.scheduling.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeroplan.scheduling.common.ErrorCode;
import com.aeroplan.scheduling.common.NotFoundException;
import com.aeroplan.scheduling.domain.LaunchPreparation;
import com.aeroplan.scheduling.domain.ReadinessStatus;
import com.aeroplan.scheduling.domain.Rocket;
import com.aeroplan.scheduling.domain.WeatherStatus;
import com.aeroplan.scheduling.domain.WorkOrder;
import com.aeroplan.scheduling.domain.WorkOrderStatus;
import com.aeroplan.scheduling.repository.LaunchPreparationRepository;
import com.aeroplan.scheduling.repository.RocketRepository;
import com.aeroplan.scheduling.repository.WorkOrderRepository;

/**
 * P0 rollup per README: a straightforward deterministic count of open/blocked work orders per
 * rocket rather than the richer Launch Readiness Agent assessment. The agent's output can later
 * enrich {@code notes}/{@code readinessScore} without changing this API shape. Weather is not
 * modeled yet (no real weather feed wired up) so {@code weatherStatus} is always
 * {@code UNKNOWN} until the agent/a real integration supplies it.
 */
@Service
public class LaunchReadinessService {
    private final RocketRepository rocketRepository;
    private final WorkOrderRepository workOrderRepository;
    private final LaunchPreparationRepository launchPreparationRepository;
    private final Clock clock;

    public LaunchReadinessService(RocketRepository rocketRepository, WorkOrderRepository workOrderRepository,
            LaunchPreparationRepository launchPreparationRepository, Clock clock) {
        this.rocketRepository = rocketRepository;
        this.workOrderRepository = workOrderRepository;
        this.launchPreparationRepository = launchPreparationRepository;
        this.clock = clock;
    }

    @Transactional
    public LaunchPreparation evaluate(Long rocketId) {
        Rocket rocket = rocketRepository.findById(rocketId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROCKET_NOT_FOUND, "Rocket " + rocketId + " was not found"));
        List<WorkOrder> workOrders = workOrderRepository.findByRocketId(rocketId);

        long total = workOrders.size();
        long complete = workOrders.stream().filter(wo -> wo.status() == WorkOrderStatus.COMPLETE).count();
        long blockedCritical = workOrders.stream()
                .filter(wo -> wo.status() == WorkOrderStatus.BLOCKED)
                .count();
        long late = workOrders.stream().filter(wo -> wo.status() == WorkOrderStatus.LATE).count();

        ReadinessStatus status;
        String notes;
        if (total == 0) {
            status = ReadinessStatus.ON_TRACK;
            notes = "No work orders tracked yet for this rocket.";
        } else if (blockedCritical > 0) {
            status = ReadinessStatus.BLOCKED;
            notes = blockedCritical + " work order(s) blocked.";
        } else if (complete == total) {
            status = ReadinessStatus.READY_FOR_LAUNCH;
            notes = "All work orders complete.";
        } else if (late > 0) {
            status = ReadinessStatus.AT_RISK;
            notes = late + " work order(s) currently late against their due date.";
        } else {
            status = ReadinessStatus.ON_TRACK;
            notes = (total - complete) + " work order(s) still open, none currently late or blocked.";
        }

        double readinessScore = total == 0 ? 100.0 : Math.round((complete * 100.0 / total) * 100) / 100.0;

        LaunchPreparation evaluation = new LaunchPreparation(rocketId, status, readinessScore, WeatherStatus.UNKNOWN,
                rocket.launchWindowStart(), notes);
        return launchPreparationRepository.save(evaluation);
    }

    public LaunchPreparation latest(Long rocketId) {
        return launchPreparationRepository.findTopByRocketIdOrderByEvaluatedAtDesc(rocketId)
                .orElseGet(() -> evaluate(rocketId));
    }
}
