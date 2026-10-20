package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
