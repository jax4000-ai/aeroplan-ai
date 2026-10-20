package com.aeroplan.scheduling.api;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequest(@NotBlank String approvedBy) {
}
