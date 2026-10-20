package com.aeroplan.scheduling.service;

import com.aeroplan.scheduling.engine.ScheduleCandidateResult;

/** A candidate schedule produced by the engine, together with the DRAFT version it was persisted as. */
public record PersistedCandidate(Long scheduleVersionId, ScheduleCandidateResult result) {
}
