package com.aeroplan.scheduling.engine;

import java.time.Instant;

/** One operation's resource assignment produced by the engine (not yet persisted). */
public record AssignmentDraft(Long operationId, Long resourceId, Instant startAt, Instant endAt) {
}
