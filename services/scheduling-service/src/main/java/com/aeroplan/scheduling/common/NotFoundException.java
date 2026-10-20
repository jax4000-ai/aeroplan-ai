package com.aeroplan.scheduling.common;

public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
