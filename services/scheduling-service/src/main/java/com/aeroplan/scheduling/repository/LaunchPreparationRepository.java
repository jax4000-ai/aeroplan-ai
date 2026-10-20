package com.aeroplan.scheduling.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.LaunchPreparation;

public interface LaunchPreparationRepository extends JpaRepository<LaunchPreparation, Long> {
    Optional<LaunchPreparation> findTopByRocketIdOrderByEvaluatedAtDesc(Long rocketId);
}
