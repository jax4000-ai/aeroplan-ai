package com.aeroplan.workorder.common;

public abstract class ApiException extends RuntimeException {
    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
