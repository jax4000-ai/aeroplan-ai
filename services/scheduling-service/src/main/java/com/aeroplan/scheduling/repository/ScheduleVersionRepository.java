package com.aeroplan.scheduling.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.ScheduleVersion;

public interface ScheduleVersionRepository extends JpaRepository<ScheduleVersion, Long> {
    Optional<ScheduleVersion> findByCurrentTrue();

    Optional<ScheduleVersion> findTopByOrderByVersionNumberDesc();
}
