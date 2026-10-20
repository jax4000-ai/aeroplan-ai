package com.aeroplan.workorder.api;

import com.aeroplan.workorder.domain.Rocket;

public record RocketResponse(
        Long id,
        String name,
        String variant,
        String configuration,
        String launchWindowStart,
        String launchWindowEnd,
        String status,
        long workOrderCount,
        long completedWorkOrderCount) {

    public static RocketResponse from(Rocket rocket, long workOrderCount, long completedWorkOrderCount) {
        return new RocketResponse(
                rocket.id(),
                rocket.name(),
                rocket.variant(),
                rocket.configuration(),
                rocket.launchWindowStart() == null ? null : rocket.launchWindowStart().toString(),
                rocket.launchWindowEnd() == null ? null : rocket.launchWindowEnd().toString(),
                rocket.status().name(),
                workOrderCount,
                completedWorkOrderCount);
    }
}
