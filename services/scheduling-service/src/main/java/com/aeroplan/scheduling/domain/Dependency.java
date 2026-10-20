package com.aeroplan.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Read-only mirror of the `dependency` table owned by work-order-service. Flat (plain
 * operation/predecessor ids, no entity navigation) since scheduling-service only needs the graph
 * edges for its topological sort, not full JPA relationship traversal.
 */
@Entity
@Table(name = "dependency")
public class Dependency {
    @EmbeddedId
    private DependencyId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    private DependencyType dependencyType;

    protected Dependency() {
    }

    public Dependency(Long operationId, Long predecessorOperationId, DependencyType dependencyType) {
        this.id = new DependencyId(operationId, predecessorOperationId);
        this.dependencyType = dependencyType;
    }

    public Long operationId() {
        return id.operationId();
    }

    public Long predecessorOperationId() {
        return id.predecessorOperationId();
    }

    public DependencyType dependencyType() {
        return dependencyType;
    }
}
