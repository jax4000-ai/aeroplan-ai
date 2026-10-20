package com.aeroplan.workorder.common;

import org.springframework.http.HttpStatus;

/** Stable, machine-readable error codes returned as the `errorCode` Problem Details field. */
public enum ErrorCode {
    ROCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Rocket not found"),
    WORK_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Work order not found"),
    CYCLIC_DEPENDENCY(HttpStatus.UNPROCESSABLE_CONTENT, "Cyclic dependency rejected"),
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
