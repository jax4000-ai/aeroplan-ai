# notification-service

Java 21 + Spring Boot. The smallest of the four Java services: receives "something happened,
tell the right people" requests from `scheduling-service` (proposal approved/rejected) and
`event-processing-service` (new disruption), and delivers them via SNS and/or a simulated Slack/
email channel.

**This file is your scope.** Do not edit `../work-order-service`, `../scheduling-service`,
`../event-processing-service`, `../agent-service`, `../../frontend`, or `../../infrastructure`.

## Source of truth

1. `../../docs/api-specification.md` — the "notification-service" section.
2. `../../docs/architecture.md` — where this service sits in the disruption-recovery flow
   (publishes to SNS → email/Slack/Teams after an approved schedule).
3. `../../docs/infrastructure-spec.md` — env var `SNS_NOTIFICATIONS_TOPIC_ARN`.

## P0 checklist

1. Maven project, Java 21, Spring Boot, Spring Web, AWS SDK v2 (`sns`), JUnit 5.
2. `POST /internal/notifications` — accepts a structured payload (see the SNS notification shape
   in `api-specification.md`'s "Events" section) from `scheduling-service`/
   `event-processing-service`, publishes it to the `aeroplan-schedule-notifications` SNS topic.
   **Also log a clearly labeled simulated "Slack message" / "email"** to stdout/structured logs
   representing what would be sent to a real channel — be explicit in this README and in the
   log line itself (e.g. `[SIMULATED SLACK]`) that this is not integrated with a real Slack
   workspace or SMTP server unless you choose to add that as a stretch goal.
3. In-memory (or a small table, if you prefer persistence — document which) log of recently sent
   notifications; `GET /api/notifications` for the dashboard's "who was told what" view (P1, but
   simple enough to include in P0 if time allows).
4. A couple of tests verifying the SNS publish call is made with the right payload shape (mock
   the SNS client) and that the simulated channel logging happens.
5. `Dockerfile`, port `8080`, `/actuator/health`.

## Definition of done for P0

`./mvnw test` passes; `docker build` succeeds; a direct `curl POST /internal/notifications` with
a sample payload results in an SNS publish (verifiable via `aws sns` CLI against a real topic, or
a mocked client in tests) and a visible simulated-channel log line.
