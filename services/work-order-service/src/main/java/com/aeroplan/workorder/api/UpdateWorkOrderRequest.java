package com.aeroplan.workorder.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import com.aeroplan.workorder.domain.Priority;
import com.aeroplan.workorder.domain.WorkOrderStatus;

/** Deliberately narrower than creation: only metadata fields are mutable through this endpoint
 * for P0. Adding/removing operations after creation is out of scope for this demo. */
public record UpdateWorkOrderRequest(
        @NotBlank @Size(max = 160) String title,
        @NotNull Priority priority,
        @NotNull Instant dueAt,
        @NotNull WorkOrderStatus status) {
}
