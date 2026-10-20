package com.aeroplan.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByRocketId(Long rocketId);
}
