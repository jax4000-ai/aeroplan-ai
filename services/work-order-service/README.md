# work-order-service

Java 21 + Spring Boot (Spring MVC + Spring Data JPA — see ADR-005 in
`docs/architecture-decisions.md`). Owns the **foundational domain data**: rockets/missions, work
orders, operations, dependencies, resources, materials/BOM, inventory reservations, and the
shared append-only audit trail. Owns the Postgres schema baseline (Flyway `V1__init.sql`) since
it owns the most foundational tables — other services add their own later migrations for their
own tables (see `docs/data-model.md`'s ownership-by-table convention; do not let another
service's migration redefine a table you own).

**This file is your scope.** Everything you build lives under this folder. Do not edit
`../scheduling-service`, `../event-processing-service`, `../notification-service`,
`../agent-service`, `../../frontend`, or `../../infrastructure` — flag cross-cutting needs
instead of reaching into those folders.

## Source of truth (read before writing code)

1. `../../docs/data-model.md` — exact Postgres DDL, with table ownership marked per section.
   Your Flyway migration (`src/main/resources/db/migration/V1__init.sql`) must match the
   **entire** DDL block (all tables, not just the ones you own) since you author the baseline
   migration that creates the whole schema — other services' later migrations only add indexes/
   tables they introduce themselves, if any.
2. `../../docs/api-specification.md` — the "work-order-service" section: every endpoint, exact
   request/response shape.
3. `../../docs/architecture-decisions.md` — ADR-005 (why four services share one database;
   respect table ownership), ADR-007 (Rocket as first-class entity; S3 for artifacts).
4. `../../docs/infrastructure-spec.md` — env vars `SPRING_DATASOURCE_URL`/`_USERNAME`/
   `_PASSWORD`, `AWS_REGION`, `S3_DOCUMENTS_BUCKET`.

## P0 checklist (build in this order)

1. Maven project, Java 21, Spring Boot (reuse version `4.1.1` for consistency with the author's
   other projects if no newer stable version is required), Spring Web, Spring Data JPA,
   PostgreSQL driver, Flyway, Validation, AWS SDK v2 (`s3` module, for P1 presigned URLs), JUnit
   5 + AssertJ + Testcontainers (Postgres) for integration tests.
2. Flyway migration creating the **full** schema from `data-model.md` (all tables — you own the
   baseline even though you don't own every table's writes).
3. Rocket CRUD (`GET /api/rockets`, `GET /api/rockets/{id}` with work-order rollup).
4. Work order + operation + dependency CRUD with cycle detection (reject a dependency that would
   create a cycle — return `422` with a clear `errorCode`).
5. Resources (`GET /api/resources`) and materials (`GET /api/materials`,
   `GET /api/materials/shortages`) read endpoints.
6. Seed data (demo Spring profile): a rocket (e.g. "New Glenn NG-4"), the 5 example work orders
   from `architecture.md`'s source material (WO-101..105) attached to it, plus enough resources/
   materials to make the demo disruption scenario (engine station downtime) work — gate behind
   a `demo` profile, mirroring the companion Mission Notes project's pattern of profile-gated
   synthetic seed data.
7. `GET /api/audit-events` (reads the shared `audit_event` table any service can write to — you
   serve the read API since you own the baseline schema; document this choice).
8. Unit tests for cycle detection and dependency-graph validation.
9. `Dockerfile` (multi-stage Maven build → slim JRE runtime image), exposing port `8080`,
   `/actuator/health` wired for the ALB target group health check.

## Definition of done for P0

`./mvnw test` passes; you can `docker build` the image; a `curl` walkthrough of create rocket →
create work order with operations/dependencies → attempt a cyclic dependency (expect `422`) →
list work orders filtered by rocket → fetch materials/shortages all work via `curl`/Postman
against a locally running instance (`./mvnw spring-boot:run` against a local Postgres, or
`docker compose up` once the compose file exists).
