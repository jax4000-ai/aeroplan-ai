package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
}
