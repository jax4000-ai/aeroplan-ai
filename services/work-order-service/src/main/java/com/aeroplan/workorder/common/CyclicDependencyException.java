package com.aeroplan.workorder.common;

public class CyclicDependencyException extends ApiException {
    public CyclicDependencyException(String message) {
        super(ErrorCode.CYCLIC_DEPENDENCY, message);
    }
}
