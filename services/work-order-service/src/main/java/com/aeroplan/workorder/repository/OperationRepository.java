package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.Operation;

public interface OperationRepository extends JpaRepository<Operation, Long> {
}
