package com.aeroplan.scheduling.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls notification-service's {@code POST /internal/notifications} endpoint rather than
 * publishing to SNS directly (the option docs/api-specification.md calls "preferred, keeps
 * delivery logic in one place"). Best-effort: a notification failure must never roll back or
 * block the proposal decision that triggered it (notification-service may not be deployed yet in
 * a local/demo run).
 */
@Component
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public NotificationClient(RestClient.Builder restClientBuilder,
            @Value("${aeroplan.notification-service.base-url:http://localhost:8083}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    public void notify(String eventType, String summary, Long rocketId) {
        try {
            restClient.post()
                    .uri(baseUrl + "/internal/notifications")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(new NotificationRequest(eventType, summary, rocketId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("notification-service unreachable ({}); continuing without delivery (eventType={})",
                    e.getMessage(), eventType);
        }
    }

    private record NotificationRequest(String eventType, String summary, Long rocketId) {
    }
}
