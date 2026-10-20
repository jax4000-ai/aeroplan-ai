package com.aeroplan.workorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.Dependency;
import com.aeroplan.workorder.domain.DependencyId;

public interface DependencyRepository extends JpaRepository<Dependency, DependencyId> {
}
