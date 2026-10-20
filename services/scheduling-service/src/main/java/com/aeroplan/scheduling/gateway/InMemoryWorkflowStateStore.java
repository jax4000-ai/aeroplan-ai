package com.aeroplan.scheduling.gateway;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Default for local dev/tests/demo: an in-memory map instead of a real DynamoDB table. */
@Component
@Profile("!aws")
public class InMemoryWorkflowStateStore implements WorkflowStateStore {
    private record Entry(Long proposalId, String taskToken, Instant updatedAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void put(String executionArn, Long proposalId, String taskToken) {
        if (executionArn == null) {
            return;
        }
        entries.put(executionArn, new Entry(proposalId, taskToken, Instant.now()));
    }

    @Override
    public Optional<String> findTaskToken(String executionArn) {
        return Optional.ofNullable(entries.get(executionArn)).map(Entry::taskToken);
    }
}
