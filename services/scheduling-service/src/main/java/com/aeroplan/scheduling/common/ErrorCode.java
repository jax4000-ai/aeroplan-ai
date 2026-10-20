package com.aeroplan.scheduling.common;

import org.springframework.http.HttpStatus;

/** Stable, machine-readable error codes returned as the `errorCode` Problem Details field. */
public enum ErrorCode {
    ROCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Rocket not found"),
    SCHEDULE_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Schedule version not found"),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Proposal not found"),
    PROPOSAL_ALREADY_DECIDED(HttpStatus.CONFLICT, "Proposal already decided"),
    REQUEST_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Bad Request"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
