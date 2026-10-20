package com.aeroplan.scheduling.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;

/**
 * Real AWS implementation, active only under the "aws" Spring profile (requires the ECS task
 * role to grant {@code states:SendTaskSuccess}/{@code states:SendTaskFailure} — see
 * docs/infrastructure-spec.md).
 */
@Component
@Profile("aws")
public class AwsStepFunctionsGateway implements StepFunctionsGateway {
    private static final Logger log = LoggerFactory.getLogger(AwsStepFunctionsGateway.class);

    private final SfnClient sfnClient;

    public AwsStepFunctionsGateway(SfnClient sfnClient) {
        this.sfnClient = sfnClient;
    }

    @Override
    public void sendTaskSuccess(String taskToken, String outputJson) {
        if (taskToken == null || taskToken.isBlank()) {
            return;
        }
        try {
            sfnClient.sendTaskSuccess(SendTaskSuccessRequest.builder().taskToken(taskToken).output(outputJson).build());
        } catch (Exception e) {
            log.warn("SendTaskSuccess failed (execution may already be closed): {}", e.getMessage());
        }
    }

    @Override
    public void sendTaskFailure(String taskToken, String errorCode, String causeMessage) {
        if (taskToken == null || taskToken.isBlank()) {
            return;
        }
        try {
            sfnClient.sendTaskFailure(SendTaskFailureRequest.builder().taskToken(taskToken).error(errorCode).cause(causeMessage).build());
        } catch (Exception e) {
            log.warn("SendTaskFailure failed (execution may already be closed): {}", e.getMessage());
        }
    }
}
