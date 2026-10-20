package com.aeroplan.scheduling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.Dependency;
import com.aeroplan.scheduling.domain.DependencyId;

public interface DependencyRepository extends JpaRepository<Dependency, DependencyId> {
    List<Dependency> findAll();
}
