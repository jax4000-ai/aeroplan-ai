package com.aeroplan.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.MachineDowntime;

public interface MachineDowntimeRepository extends JpaRepository<MachineDowntime, Long> {
}
