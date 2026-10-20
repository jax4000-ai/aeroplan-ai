package com.aeroplan.workorder.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aeroplan.workorder.repository.AuditEventRepository;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {
    private final AuditEventRepository auditEventRepository;

    public AuditEventController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public List<AuditEventResponse> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "50") int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 200), org.springframework.data.domain.Sort.by("occurredAt").descending());
        var page = entityType != null && entityId != null
                ? auditEventRepository.findAllByEntityTypeAndEntityId(entityType, entityId, pageable)
                : entityType != null
                        ? auditEventRepository.findAllByEntityType(entityType, pageable)
                        : auditEventRepository.findAll(pageable);
        return page.map(AuditEventResponse::from).toList();
    }
}
