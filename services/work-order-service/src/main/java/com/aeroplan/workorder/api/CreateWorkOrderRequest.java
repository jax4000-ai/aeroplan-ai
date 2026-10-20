package com.aeroplan.workorder.api;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.aeroplan.workorder.domain.Priority;
import com.aeroplan.workorder.domain.WorkType;

public record CreateWorkOrderRequest(
        @NotNull Long rocketId,
        @NotBlank @Size(max = 64) String partNumber,
        @NotBlank @Size(max = 160) String title,
        @NotNull WorkType workType,
        @NotNull Priority priority,
        @NotNull Instant dueAt,
        List<@Valid OperationRequest> operations) {

    public record OperationRequest(
            @NotBlank @Size(max = 160) String name,
            @jakarta.validation.constraints.Positive int durationMinutes,
            @NotBlank @Size(max = 64) String requiredSkill,
            @NotBlank @Size(max = 64) String requiredResourceType,
            int sequenceIndex,
            Integer dependsOnSequenceIndex) {
    }
}
