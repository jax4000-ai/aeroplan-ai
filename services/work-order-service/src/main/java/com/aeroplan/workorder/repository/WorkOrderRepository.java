package com.aeroplan.workorder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByRocket_Id(Long rocketId);
}
