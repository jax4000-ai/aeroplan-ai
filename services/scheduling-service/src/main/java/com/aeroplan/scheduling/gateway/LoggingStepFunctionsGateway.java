package com.aeroplan.scheduling.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default implementation for local dev/tests/demo: logs the intent instead of calling AWS. The
 * API transaction (publish/reject + audit event) is the authoritative action regardless — this
 * callback only closes out the Step Functions execution history, so it's safe to no-op when
 * Step Functions isn't actually running this workflow.
 */
@Component
@Profile("!aws")
public class LoggingStepFunctionsGateway implements StepFunctionsGateway {
    private static final Logger log = LoggerFactory.getLogger(LoggingStepFunctionsGateway.class);

    @Override
    public void sendTaskSuccess(String taskToken, String outputJson) {
        if (taskToken == null || taskToken.isBlank()) {
            return;
        }
        log.info("[stub] SendTaskSuccess taskToken={} output={}", redact(taskToken), outputJson);
    }

    @Override
    public void sendTaskFailure(String taskToken, String errorCode, String causeMessage) {
        if (taskToken == null || taskToken.isBlank()) {
            return;
        }
        log.info("[stub] SendTaskFailure taskToken={} errorCode={} cause={}", redact(taskToken), errorCode, causeMessage);
    }

    private String redact(String taskToken) {
        return taskToken.length() <= 8 ? "***" : taskToken.substring(0, 8) + "...";
    }
}
