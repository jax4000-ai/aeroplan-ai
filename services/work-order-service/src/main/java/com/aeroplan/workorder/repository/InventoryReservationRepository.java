package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.InventoryReservation;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
}
