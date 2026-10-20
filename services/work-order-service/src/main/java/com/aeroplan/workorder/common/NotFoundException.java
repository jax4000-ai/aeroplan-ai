package com.aeroplan.workorder.common;

public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    public static NotFoundException rocket(Long id) {
        return new NotFoundException(ErrorCode.ROCKET_NOT_FOUND, "Rocket " + id + " was not found");
    }

    public static NotFoundException workOrder(Long id) {
        return new NotFoundException(ErrorCode.WORK_ORDER_NOT_FOUND, "Work order " + id + " was not found");
    }
}
