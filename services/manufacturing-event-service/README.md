# manufacturing-event-service

Java 21 + Spring Boot. A small, focused service with one job: reliably turn a
`MACHINE_DOWNTIME` event (from SQS or direct HTTP POST) into a persisted `machine_downtime` row
and a Step Functions execution — exactly once, even under SQS at-least-once delivery, consumer
crashes, and duplicate events.

**This file is your scope.** Do not edit `../scheduling-service`, `../agent-service`,
`../../frontend`, or `../../infrastructure`.

## Source of truth

1. `../../docs/api-specification.md` — the "manufacturing-event-service" section and the SQS
   message shape.
2. `../../docs/data-model.md` — the `machine_downtime` table and the `IdempotencyRecords`
   DynamoDB table.
3. `../../docs/architecture.md` — "Disruption recovery flow," step 2–3.
4. `../../docs/infrastructure-spec.md` — env vars `SQS_DOWNTIME_QUEUE_URL`,
   `DYNAMODB_IDEMPOTENCY_TABLE`, `STEP_FUNCTIONS_STATE_MACHINE_ARN`.

## P0 checklist

1. Maven project, Java 21, Spring Boot, Spring Web (for the alternate HTTP ingestion path),
   Spring Data JPA (read access to `resource` table to resolve `resourceId` by name; write
   access to `machine_downtime` — same RDS instance/credentials as `scheduling-service`, but
   this service should touch only these two concerns, not the full schema), AWS SDK v2
   (`sqs`, `dynamodb`, `sfn`), JUnit 5 + Testcontainers.
2. SQS listener (Spring Cloud AWS `@SqsListener` or a manual `SqsAsyncClient` poller — your
   choice, document which and why) consuming `manufacturing-downtime-events`:
   - Look up `eventId` in `IdempotencyRecords`; if `COMPLETED`, ack and return.
   - Write (or upsert) the idempotency record as `PROCESSING` first.
   - Resolve `resourceId` (name) → `resource.id`, insert `machine_downtime`
     (`event_id` unique constraint as a second guard against races).
   - Call Step Functions `StartExecution` on `aeroplan-disruption-recovery` with a JSON input
     containing `downtimeEvent`, `machineDowntimeId`, and the current published
     `schedule_version.id` (read-only query).
   - Mark the idempotency record `COMPLETED`.
   - Any exception before the final step: let the SQS message become visible again (do not
     manually delete it) so it redelivers; after 5 attempts (queue's `maxReceiveCount`) it
     routes to the DLQ automatically — you don't need to implement that part, just don't swallow
     exceptions.
3. `POST /api/downtime-events` — same logic as the SQS path, for manual/demo testing without
   needing to publish to SQS by hand.
4. A small local downtime **simulator**: a CLI script or a `@Scheduled`/manual-trigger endpoint
   (behind a `demo` profile) that publishes a couple of realistic `MACHINE_DOWNTIME` events to
   SQS — useful for your own testing and for the live demo.
5. Integration tests proving: (a) the same `eventId` delivered twice produces exactly one
   `machine_downtime` row and one Step Functions execution, (b) a message that fails before the
   idempotency record is marked `COMPLETED` is safe to redeliver.
6. `Dockerfile`, port `8081`, `/actuator/health`.

## Definition of done for P0

`./mvnw test` passes, including the duplicate-event and crash-recovery tests described above.
You can run this service locally against LocalStack or a real (low-volume, cheap) SQS
queue/DynamoDB table and watch a `MACHINE_DOWNTIME` message turn into exactly one downtime
record and one Step Functions execution start.
