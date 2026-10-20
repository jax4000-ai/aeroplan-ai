# scheduling-service

Java 21 + Spring Boot (Spring MVC + Spring Data JPA, **not** WebFlux — see ADR-005 in
`docs/architecture-decisions.md` for why). Owns the system of record in PostgreSQL: work
orders, operations, dependencies, resources, materials, schedule versions/assignments,
proposals, and the audit trail. Implements the deterministic scheduling engine and every
public/internal REST endpoint listed under "scheduling-service" in
`../../docs/api-specification.md`.

**This file is your scope.** Everything you build lives under this folder. Do not edit
`../manufacturing-event-service`, `../agent-service`, `../../frontend`, or
`../../infrastructure` — flag cross-cutting needs (e.g. "I need this env var name") instead of
reaching into those folders.

## Source of truth (read before writing code)

1. `../../docs/data-model.md` — exact Postgres DDL. Your Flyway migration
   (`src/main/resources/db/migration/V1__init.sql`) must match it.
2. `../../docs/api-specification.md` — every endpoint, request/response shape, and the
   `/internal/proposals` + approve/reject + `SendTaskSuccess` contract.
3. `../../docs/architecture-decisions.md` — ADR-001 (Postgres vs DynamoDB split), ADR-002 (Step
   Functions callback pattern), ADR-003 (agents never invent feasibility — your validation must
   enforce this).
4. `../../docs/infrastructure-spec.md` — the exact environment variable names you must read
   config from (`SPRING_DATASOURCE_URL`, `DYNAMODB_WORKFLOW_TABLE`,
   `SNS_NOTIFICATIONS_TOPIC_ARN`, `STEP_FUNCTIONS_STATE_MACHINE_ARN`, `AWS_REGION`).

## P0 checklist (build in this order)

1. Maven project, Java 21, Spring Boot (reuse version `4.1.1` for consistency with the author's
   other projects if no newer stable version is required), Spring Web, Spring Data JPA,
   PostgreSQL driver, Flyway, Validation, AWS SDK v2 (`dynamodb`, `sns`, `sfn` modules), JUnit 5
   + AssertJ + Testcontainers (Postgres) for integration tests.
2. Flyway migration matching `data-model.md` exactly.
3. Work order + operation + dependency CRUD with cycle detection (reject a dependency that would
   create a cycle — return `422` with a clear `errorCode`).
4. Seed data: the 5 example work orders from `architecture.md`'s source material (WO-101..105)
   plus enough resources/materials to make the demo scenario (engine station downtime) work —
   add this as either a Flyway `V2__seed_demo_data.sql` gated by a Spring profile (`demo`) or a
   `CommandLineRunner` guarded by `@Profile("demo")`, mirroring the companion Mission Notes
   project's pattern of profile-gated synthetic seed data.
5. Deterministic scheduling engine: topological sort of the dependency graph, priority queue
   (CRITICAL > HIGH > MEDIUM > LOW, tie-break by `due_at`), greedy resource allocation respecting
   `resource.capacity` and `availability_status`, earliest-feasible-start accounting for
   dependencies + any `unavailableResourceIds`/`unavailableUntil` passed to `/optimize`. Compute
   the objective score from `data-model.md`'s weighted formula (late hours, overtime, schedule
   changes — pick reasonable default weights and make them configurable). Return 1–3 candidates
   (e.g. "minimize lateness" vs "minimize overtime" as different weight profiles) — do not persist
   as `PUBLISHED`.
6. `/api/disruptions`, `/internal/proposals` (+ `waitForTaskToken` storage), `/approve`,
   `/reject` (+ `SendTaskSuccess`/`SendTaskFailure` calls, optimistic `WHERE status='PENDING'`
   conditional update, `409` on conflict), `/internal/proposals/expire`.
7. Audit event writes on every state-changing action; `GET /api/audit-events`.
8. Unit tests for: cycle detection, scheduling correctness (no dependency violations, no
   resource double-booking), optimistic-locking conflict on double-approve.
9. `Dockerfile` (multi-stage Maven build → slim JRE runtime image), exposing port `8080`,
   `/actuator/health` wired for the ALB target group health check.

## Definition of done for P0

`./mvnw test` passes; you can `docker build` the image; a `curl` walkthrough of create
work-order → optimize → create a disruption via `/api/disruptions` → fetch the resulting
proposal (even with a stubbed/mock agent response if `agent-service` isn't ready yet — don't
block on it, use a feature flag or a simple fallback summary string) → approve it → see a new
published `ScheduleVersion` and an `AuditEvent` trail, all work via `curl`/Postman against a
locally running instance (`docker compose up` once the compose file exists, or
`./mvnw spring-boot:run` against a local Postgres).
