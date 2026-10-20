package com.aeroplan.scheduling.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.scheduling.service.LaunchReadinessService;

@RestController
public class LaunchReadinessController {
    private final LaunchReadinessService launchReadinessService;

    public LaunchReadinessController(LaunchReadinessService launchReadinessService) {
        this.launchReadinessService = launchReadinessService;
    }

    @GetMapping("/api/launch-readiness")
    public LaunchReadinessResponse readiness(@RequestParam Long rocketId) {
        return LaunchReadinessResponse.from(launchReadinessService.latest(rocketId));
    }
}
