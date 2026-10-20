package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.Rocket;

public interface RocketRepository extends JpaRepository<Rocket, Long> {
}
