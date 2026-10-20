# scheduling-service

Java 21 + Spring Boot (Spring MVC + Spring Data JPA — see ADR-005 in
`docs/architecture-decisions.md`). Owns the deterministic scheduling engine, schedule versions/
assignments, schedule proposals (the disruption-recovery approval workflow), and launch-
readiness rollups. Reads `work_order`/`operation`/`dependency`/`resource` (owned by
`work-order-service`) but does not write them.

**This file is your scope.** Do not edit `../work-order-service`, `../event-processing-service`,
`../notification-service`, `../agent-service`, `../../frontend`, or `../../infrastructure`.

## Source of truth

1. `../../docs/data-model.md` — the `schedule_version`, `schedule_assignment`,
   `schedule_proposal`, and `launch_preparation` tables are yours to write; everything else in
   this file you only read.
2. `../../docs/api-specification.md` — the "scheduling-service" section: every endpoint, exact
   request/response shapes, and the `/internal/proposals` + approve/reject +
   `SendTaskSuccess`/`SendTaskFailure` contract.
3. `../../docs/architecture-decisions.md` — ADR-001 (Postgres vs DynamoDB split), ADR-002 (Step
   Functions callback pattern), ADR-003 (agents never invent feasibility — your validation must
   enforce this), ADR-005 (shared database, table ownership).
4. `../../docs/infrastructure-spec.md` — env vars `SPRING_DATASOURCE_URL`,
   `DYNAMODB_WORKFLOW_TABLE`, `SNS_NOTIFICATIONS_TOPIC_ARN` or `NOTIFICATION_SERVICE_BASE_URL`
   (pick one delivery path and document it), `STEP_FUNCTIONS_STATE_MACHINE_ARN`, `AWS_REGION`.

## P0 checklist (build in this order)

1. Maven project, Java 21, Spring Boot, Spring Web, Spring Data JPA, PostgreSQL driver, Flyway
   (a migration that only adds your own tables if `work-order-service`'s baseline hasn't created
   them yet in your test context — document how you handle this; simplest is to also run
   `work-order-service`'s `V1__init.sql` as your own test fixture, since it's the shared schema),
   Validation, AWS SDK v2 (`dynamodb`, `sns` if used, `sfn` modules), JUnit 5 + AssertJ +
   Testcontainers (Postgres).
2. Deterministic scheduling engine: topological sort of the dependency graph, priority queue
   (CRITICAL > HIGH > MEDIUM > LOW, tie-break by `due_at`), greedy resource allocation respecting
   `resource.capacity` and `availability_status`, earliest-feasible-start accounting for
   dependencies + any `unavailableResourceIds`/`unavailableUntil` passed to `/optimize`. Compute
   the objective score (late hours, overtime, schedule changes — pick reasonable default weights
   and make them configurable). Return 1–3 candidates — do not persist as `PUBLISHED`.
3. `GET /api/schedules/current`, `GET /api/schedules/{versionId}`.
4. `/internal/proposals` (+ `waitForTaskToken` storage in Postgres and DynamoDB
   `WorkflowState`), `GET /api/proposals`, `GET /api/proposals/{id}`, `/approve`, `/reject`
   (+ `SendTaskSuccess`/`SendTaskFailure` calls, optimistic `WHERE status='PENDING'` conditional
   update, `409` on conflict), `/internal/proposals/expire`.
5. On approval, call `notification-service`'s internal endpoint (preferred, keeps delivery logic
   in one place) **or** publish directly to SNS — pick one and document it in this README; also
   write the `AuditEvent`.
6. `GET /api/launch-readiness` — for P0 this can be a straightforward deterministic rollup
   (count of open/blocked work orders per rocket, any unresolved disruptions, days until launch
   window) rather than waiting on the Launch Readiness Agent's richer assessment; the agent's
   output can enrich `launch_preparation.notes`/`readiness_score` later without changing the API
   shape.
7. Unit tests for: scheduling correctness (no dependency violations, no resource double-
   booking), optimistic-locking conflict on double-approve, cycle-free topological sort.
8. `Dockerfile`, port `8080` (note: if you run this alongside `work-order-service` locally,
   they'll need different host ports in `docker-compose.yml`, owned by the integration pass —
   just ensure your container's internal port is `8080` and your `/actuator/health` works),
   `/actuator/health`.

Since `event-processing-service` and `agent-service` may not be ready yet, don't block on them —
for `/internal/proposals`, build it to accept whatever `agentFindings` JSON it's given (even a
stubbed/empty one for your own manual testing) rather than depending on the real services being
up, and you can call `/api/disruptions`-equivalent test fixtures directly in your own tests
rather than requiring a live SQS message.

## Definition of done for P0

`./mvnw test` passes; `docker build` succeeds; a `curl` walkthrough of optimize → manually
POST to `/internal/proposals` with a fabricated task token/findings (simulating what Step
Functions will eventually send) → fetch the proposal → approve it → see a new published
`ScheduleVersion` and an `AuditEvent` all work locally.
