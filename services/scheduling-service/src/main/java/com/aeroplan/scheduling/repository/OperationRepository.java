package com.aeroplan.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.Operation;

public interface OperationRepository extends JpaRepository<Operation, Long> {
    List<Operation> findByWorkOrderIdIn(List<Long> workOrderIds);
}
