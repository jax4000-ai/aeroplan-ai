package com.aeroplan.scheduling.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.scheduling.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
