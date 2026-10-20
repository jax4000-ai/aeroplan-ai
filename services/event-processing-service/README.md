# event-processing-service

Java 21 + Spring Boot. A small, focused service with one job: reliably turn a manufacturing
event — `MACHINE_DOWNTIME` or `TEST_RESULT` (from SQS or direct HTTP POST) — into a persisted
row and, for downtime events, a Step Functions execution — exactly once, even under SQS
at-least-once delivery, consumer crashes, and duplicate events.

**This file is your scope.** Do not edit `../work-order-service`, `../scheduling-service`,
`../notification-service`, `../agent-service`, `../../frontend`, or `../../infrastructure`.

## Source of truth

1. `../../docs/api-specification.md` — the "event-processing-service" section and the SQS
   message shapes for both event types.
2. `../../docs/data-model.md` — the `machine_downtime` and `test_result` tables (yours to
   write), and the `IdempotencyRecords` DynamoDB table.
3. `../../docs/architecture.md` — "Disruption recovery flow."
4. `../../docs/infrastructure-spec.md` — env vars `SQS_MANUFACTURING_EVENTS_QUEUE_URL`,
   `DYNAMODB_IDEMPOTENCY_TABLE`, `STEP_FUNCTIONS_STATE_MACHINE_ARN`, `S3_DOCUMENTS_BUCKET`.

## P0 checklist

1. Maven project, Java 21, Spring Boot, Spring Web (for the alternate HTTP ingestion paths),
   Spring Data JPA (read access to `resource` and `work_order` tables to resolve
   names/dependencies; write access to `machine_downtime` and `test_result` only — same RDS
   instance/credentials as the other services, but touch only these two tables plus read-only
   lookups), AWS SDK v2 (`sqs`, `dynamodb`, `sfn`, `s3`), JUnit 5 + Testcontainers.
2. SQS listener (Spring Cloud AWS `@SqsListener` or a manual `SqsAsyncClient` poller — your
   choice, document which and why) consuming `aeroplan-manufacturing-events`, discriminating on
   `eventType`:
   - **`MACHINE_DOWNTIME`:** look up `eventId` in `IdempotencyRecords`; if `COMPLETED`, ack and
     return. Write the idempotency record as `PROCESSING`. Resolve `resourceId` (name) →
     `resource.id`, insert `machine_downtime` (`event_id` unique constraint as a second guard).
     Call Step Functions `StartExecution` on `aeroplan-disruption-recovery` with a JSON input
     containing `downtimeEvent`, `machineDowntimeId`, `affectedRocketIds` (derived by joining
     through affected work orders' `rocket_id`), and the current published
     `schedule_version.id`. Mark the idempotency record `COMPLETED`.
   - **`TEST_RESULT`:** same idempotency pattern, insert `test_result`. For P0, a `FAIL` result
     can optionally also trigger a Step Functions execution (treat it like a disruption) — this
     is a reasonable P1 extension if time allows; for P0 it's sufficient to just persist the
     result and make it queryable.
   - Any exception before marking `COMPLETED`: let the SQS message become visible again (do not
     manually delete it) so it redelivers; after 5 attempts (queue's `maxReceiveCount`) it
     routes to the DLQ automatically — you don't need to implement that part, just don't swallow
     exceptions.
3. `POST /api/disruptions` and `POST /api/test-results` — same logic as the SQS path, for
   manual/demo testing without needing to publish to SQS by hand. `GET /api/test-results` for
   querying.
4. A small local event **simulator**: a CLI script or a `@Scheduled`/manual-trigger endpoint
   (behind a `demo` profile) that publishes a couple of realistic `MACHINE_DOWNTIME` and
   `TEST_RESULT` events to SQS — useful for your own testing and for the live demo.
5. (Optional, demonstrates the S3 pattern from ADR-007) write a small synthetic JSON/text "test
   report" to the `S3_DOCUMENTS_BUCKET` when recording a `TEST_RESULT`, storing the resulting
   object key in `test_result.artifact_s3_key`.
6. Integration tests proving: (a) the same `eventId` delivered twice produces exactly one row
   and (for downtime events) one Step Functions execution, (b) a message that fails before the
   idempotency record is marked `COMPLETED` is safe to redeliver.
7. `Dockerfile`, port `8080` (containers are isolated — internal port collisions across services
   don't matter; only the integration pass's `docker-compose.yml` needs distinct host ports),
   `/actuator/health`.

## Definition of done for P0

`./mvnw test` passes, including the duplicate-event and crash-recovery tests described above.
You can run this service locally against LocalStack or a real (low-volume, cheap) SQS
queue/DynamoDB table and watch a `MACHINE_DOWNTIME` message turn into exactly one downtime
record and one Step Functions execution start, and a `TEST_RESULT` message turn into exactly one
recorded result.
