package com.aeroplan.workorder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.MapsId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "dependency")
public class Dependency {
    @EmbeddedId
    private DependencyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("operationId")
    @JoinColumn(name = "operation_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Operation operation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("predecessorOperationId")
    @JoinColumn(name = "predecessor_operation_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Operation predecessorOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    private DependencyType dependencyType;

    protected Dependency() {
    }

    public Dependency(Operation operation, Operation predecessorOperation, DependencyType dependencyType) {
        this.id = new DependencyId(operation.id(), predecessorOperation.id());
        this.operation = operation;
        this.predecessorOperation = predecessorOperation;
        this.dependencyType = dependencyType;
    }

    public Operation operation() {
        return operation;
    }

    public Operation predecessorOperation() {
        return predecessorOperation;
    }

    public DependencyType dependencyType() {
        return dependencyType;
    }
}
