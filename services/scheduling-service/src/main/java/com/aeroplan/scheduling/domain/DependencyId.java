package com.aeroplan.scheduling.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class DependencyId implements Serializable {
    private Long operationId;
    private Long predecessorOperationId;

    protected DependencyId() {
    }

    public DependencyId(Long operationId, Long predecessorOperationId) {
        this.operationId = operationId;
        this.predecessorOperationId = predecessorOperationId;
    }

    public Long operationId() {
        return operationId;
    }

    public Long predecessorOperationId() {
        return predecessorOperationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyId that)) return false;
        return Objects.equals(operationId, that.operationId) && Objects.equals(predecessorOperationId, that.predecessorOperationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId, predecessorOperationId);
    }
}
