package com.aeroplan.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.Rocket;

public interface RocketRepository extends JpaRepository<Rocket, Long> {
    List<Rocket> findAll();
}
