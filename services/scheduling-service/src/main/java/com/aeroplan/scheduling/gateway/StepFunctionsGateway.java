package com.aeroplan.scheduling.gateway;

/**
 * Resolves the stored {@code waitForTaskToken} back to a live Step Functions execution. Both the
 * real AWS SDK implementation and a local logging stub implement this so {@code ProposalService}
 * never has to know which one is active (see README: "aws" Spring profile toggles between them).
 */
public interface StepFunctionsGateway {
    void sendTaskSuccess(String taskToken, String outputJson);

    void sendTaskFailure(String taskToken, String errorCode, String causeMessage);
}
