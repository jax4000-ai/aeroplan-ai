package com.aeroplan.workorder.service;

import java.time.Clock;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.aeroplan.workorder.domain.AuditEvent;
import com.aeroplan.workorder.repository.AuditEventRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(String eventType, String entityType, Long entityId, String actor, Map<String, Object> detail) {
        String json;
        try {
            json = objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            json = "{}";
        }
        repository.save(new AuditEvent(eventType, entityType, entityId, actor, json, clock.instant()));
    }
}
