package com.aeroplan.scheduling.gateway;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Real DynamoDB-backed implementation, active only under the "aws" Spring profile. */
@Component
@Profile("aws")
public class DynamoDbWorkflowStateStore implements WorkflowStateStore {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbWorkflowStateStore(DynamoDbClient dynamoDbClient,
            org.springframework.core.env.Environment environment) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = environment.getProperty("aeroplan.dynamodb.workflow-state-table", "WorkflowState");
    }

    @Override
    public void put(String executionArn, Long proposalId, String taskToken) {
        if (executionArn == null) {
            return;
        }
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("executionArn", AttributeValue.fromS(executionArn));
        item.put("proposalId", AttributeValue.fromN(String.valueOf(proposalId)));
        if (taskToken != null) {
            item.put("taskToken", AttributeValue.fromS(taskToken));
        }
        item.put("updatedAt", AttributeValue.fromS(Instant.now().toString()));
        dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    @Override
    public Optional<String> findTaskToken(String executionArn) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("executionArn", AttributeValue.fromS(executionArn)))
                .build());
        if (!response.hasItem()) {
            return Optional.empty();
        }
        AttributeValue token = response.item().get("taskToken");
        return token == null ? Optional.empty() : Optional.ofNullable(token.s());
    }
}
