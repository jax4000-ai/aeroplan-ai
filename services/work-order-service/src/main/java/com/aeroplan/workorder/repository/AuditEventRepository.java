package com.aeroplan.workorder.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aeroplan.workorder.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    Page<AuditEvent> findAllByEntityType(String entityType, Pageable pageable);
}
