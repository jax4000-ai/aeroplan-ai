package com.aeroplan.scheduling.api;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String rejectedBy) {
}
