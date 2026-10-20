package com.aeroplan.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
