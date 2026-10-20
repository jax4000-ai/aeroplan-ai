package com.aeroplan.scheduling.api;

import java.time.Instant;

import com.aeroplan.scheduling.domain.LaunchPreparation;

public record LaunchReadinessResponse(
        Long rocketId,
        String readinessStatus,
        Double readinessScore,
        String weatherStatus,
        Instant plannedLaunchAt,
        String notes) {

    public static LaunchReadinessResponse from(LaunchPreparation preparation) {
        return new LaunchReadinessResponse(preparation.rocketId(), preparation.readinessStatus().name(),
                preparation.readinessScore(), preparation.weatherStatus() == null ? null : preparation.weatherStatus().name(),
                preparation.plannedLaunchAt(), preparation.notes());
    }
}
