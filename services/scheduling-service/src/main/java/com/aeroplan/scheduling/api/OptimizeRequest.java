package com.aeroplan.scheduling.api;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record OptimizeRequest(
        @NotBlank String reason,
        List<Long> unavailableResourceIds,
        Instant unavailableUntil) {
}
