package com.aeroplan.workorder.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aeroplan.workorder.api.RocketResponse;
import com.aeroplan.workorder.common.NotFoundException;
import com.aeroplan.workorder.domain.Rocket;
import com.aeroplan.workorder.domain.WorkOrder;
import com.aeroplan.workorder.domain.WorkOrderStatus;
import com.aeroplan.workorder.repository.RocketRepository;
import com.aeroplan.workorder.repository.WorkOrderRepository;

@Service
public class RocketService {
    private final RocketRepository rocketRepository;
    private final WorkOrderRepository workOrderRepository;

    public RocketService(RocketRepository rocketRepository, WorkOrderRepository workOrderRepository) {
        this.rocketRepository = rocketRepository;
        this.workOrderRepository = workOrderRepository;
    }

    public List<Rocket> list() {
        return rocketRepository.findAll();
    }

    public Rocket get(Long id) {
        return rocketRepository.findById(id).orElseThrow(() -> NotFoundException.rocket(id));
    }

    public RocketResponse toResponse(Rocket rocket) {
        List<WorkOrder> workOrders = workOrderRepository.findAllByRocket_Id(rocket.id());
        long completed = workOrders.stream().filter(w -> w.status() == WorkOrderStatus.COMPLETE).count();
        return RocketResponse.from(rocket, workOrders.size(), completed);
    }
}
