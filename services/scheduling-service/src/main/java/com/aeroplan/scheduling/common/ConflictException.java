package com.aeroplan.scheduling.common;

/** Thrown when an optimistic conditional update (e.g. decide-if-pending) affects zero rows. */
public class ConflictException extends ApiException {
    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
