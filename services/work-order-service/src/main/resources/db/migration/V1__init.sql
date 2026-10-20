-- Baseline schema for AeroPlan AI. Owned by work-order-service (this is the authoritative
-- migration referenced by docs/data-model.md — keep them in sync). Table ownership comments
-- below indicate which service writes each table; other services only read tables they do not
-- own (see docs/architecture-decisions.md ADR-005).

-- ============================================================================
-- Owned by: work-order-service
-- ============================================================================

CREATE TABLE rocket (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL,
    variant             VARCHAR(80),
    configuration       VARCHAR(120),
    launch_window_start TIMESTAMPTZ,
    launch_window_end   TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL CHECK (status IN ('IN_PRODUCTION','INTEGRATION','TEST','READY','LAUNCHED','SCRUBBED'))
);

CREATE TABLE work_order (
    id              BIGSERIAL PRIMARY KEY,
    rocket_id       BIGINT NOT NULL REFERENCES rocket(id),
    part_number     VARCHAR(64) NOT NULL,
    title           VARCHAR(160) NOT NULL,
    work_type       VARCHAR(32) NOT NULL CHECK (work_type IN ('MANUFACTURING','ASSEMBLY','INTEGRATION','TEST','LAUNCH_PREP')),
    priority        VARCHAR(16) NOT NULL CHECK (priority IN ('CRITICAL','HIGH','MEDIUM','LOW')),
    due_at          TIMESTAMPTZ NOT NULL,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETE','LATE','BLOCKED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE operation (
    id                      BIGSERIAL PRIMARY KEY,
    work_order_id           BIGINT NOT NULL REFERENCES work_order(id) ON DELETE CASCADE,
    name                    VARCHAR(160) NOT NULL,
    duration_minutes        INT NOT NULL CHECK (duration_minutes > 0),
    required_skill          VARCHAR(64) NOT NULL,
    required_resource_type  VARCHAR(64) NOT NULL,
    sequence_index          INT NOT NULL,
    status                  VARCHAR(16) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETE','BLOCKED'))
);

CREATE TABLE dependency (
    operation_id             BIGINT NOT NULL REFERENCES operation(id) ON DELETE CASCADE,
    predecessor_operation_id BIGINT NOT NULL REFERENCES operation(id) ON DELETE CASCADE,
    dependency_type          VARCHAR(20) NOT NULL DEFAULT 'FINISH_START' CHECK (dependency_type IN ('FINISH_START','FINISH_FINISH')),
    PRIMARY KEY (operation_id, predecessor_operation_id),
    CHECK (operation_id <> predecessor_operation_id)
);

-- `skills` is stored as a comma-separated string rather than a native TEXT[] (documented
-- simplification vs. docs/data-model.md for cross-database portability with H2 in local dev;
-- production Postgres could use a real array, but this keeps one JPA mapping for both profiles).
CREATE TABLE resource (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    resource_type       VARCHAR(64) NOT NULL,
    skills              VARCHAR(500) NOT NULL DEFAULT '',
    capacity            INT NOT NULL DEFAULT 1,
    availability_status VARCHAR(16) NOT NULL CHECK (availability_status IN ('AVAILABLE','DOWN','MAINTENANCE'))
);

CREATE TABLE material (
    id                  BIGSERIAL PRIMARY KEY,
    part_number         VARCHAR(64) NOT NULL UNIQUE,
    description         VARCHAR(200) NOT NULL,
    quantity_on_hand    INT NOT NULL DEFAULT 0,
    quantity_on_order   INT NOT NULL DEFAULT 0,
    supplier            VARCHAR(120),
    expected_at         TIMESTAMPTZ,
    status              VARCHAR(16) NOT NULL DEFAULT 'IN_STOCK' CHECK (status IN ('IN_STOCK','LOW','BACKORDERED'))
);

CREATE TABLE inventory_reservation (
    id              BIGSERIAL PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_order(id),
    material_id     BIGINT NOT NULL REFERENCES material(id),
    operation_id    BIGINT REFERENCES operation(id),
    quantity        INT NOT NULL CHECK (quantity > 0),
    status          VARCHAR(16) NOT NULL DEFAULT 'RESERVED' CHECK (status IN ('RESERVED','ALLOCATED','RELEASED')),
    reserved_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- Owned by: event-processing-service (created here since schedule_proposal below references it)
-- ============================================================================

CREATE TABLE machine_downtime (
    id                      BIGSERIAL PRIMARY KEY,
    event_id                VARCHAR(80) NOT NULL UNIQUE,
    resource_id             BIGINT REFERENCES resource(id),
    started_at              TIMESTAMPTZ NOT NULL,
    estimated_recovery_at   TIMESTAMPTZ,
    reason                  VARCHAR(200),
    status                  VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED')),
    received_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE test_result (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(80) NOT NULL UNIQUE,
    work_order_id   BIGINT REFERENCES work_order(id),
    test_type       VARCHAR(80) NOT NULL,
    result          VARCHAR(16) NOT NULL CHECK (result IN ('PASS','FAIL','CONDITIONAL')),
    performed_at    TIMESTAMPTZ NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'RECORDED' CHECK (status IN ('RECORDED','REVIEWED')),
    artifact_s3_key VARCHAR(400),
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- Owned by: scheduling-service
-- ============================================================================

CREATE TABLE schedule_version (
    id              BIGSERIAL PRIMARY KEY,
    version_number  INT NOT NULL,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT','PROPOSED','PUBLISHED','SUPERSEDED','REJECTED')),
    is_current      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_by     VARCHAR(80),
    objective_score NUMERIC(12,4),
    UNIQUE (version_number)
);

CREATE TABLE schedule_assignment (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_version_id BIGINT NOT NULL REFERENCES schedule_version(id) ON DELETE CASCADE,
    operation_id        BIGINT NOT NULL REFERENCES operation(id),
    resource_id         BIGINT NOT NULL REFERENCES resource(id),
    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETE')),
    UNIQUE (schedule_version_id, operation_id)
);

-- `agent_findings` is stored as TEXT (serialized JSON via the application layer) rather than
-- native JSONB, so the same schema/JPA mapping works for both H2 (local dev) and PostgreSQL
-- (production) without dialect-specific JSON operators — documented simplification vs.
-- docs/data-model.md's JSONB type.
CREATE TABLE schedule_proposal (
    id                              BIGSERIAL PRIMARY KEY,
    machine_downtime_id             BIGINT REFERENCES machine_downtime(id),
    based_on_schedule_version_id    BIGINT NOT NULL REFERENCES schedule_version(id),
    candidate_schedule_version_id   BIGINT REFERENCES schedule_version(id),
    status                          VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
    agent_summary                   TEXT,
    agent_findings                  TEXT NOT NULL DEFAULT '{}',
    step_functions_task_token       TEXT,
    step_functions_execution_arn    TEXT,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at                      TIMESTAMPTZ,
    decided_by                      VARCHAR(80)
);

CREATE TABLE launch_preparation (
    id                  BIGSERIAL PRIMARY KEY,
    rocket_id           BIGINT NOT NULL REFERENCES rocket(id),
    readiness_status    VARCHAR(20) NOT NULL CHECK (readiness_status IN ('ON_TRACK','AT_RISK','BLOCKED','READY_FOR_LAUNCH')),
    readiness_score     NUMERIC(5,2),
    weather_status      VARCHAR(20) CHECK (weather_status IN ('GO','NO_GO','UNKNOWN')),
    planned_launch_at   TIMESTAMPTZ,
    evaluated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes               TEXT
);

-- ============================================================================
-- Shared / cross-cutting — owned by work-order-service (append-only, every service may write)
-- ============================================================================

CREATE TABLE audit_event (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(64) NOT NULL,
    entity_type     VARCHAR(64) NOT NULL,
    entity_id       BIGINT,
    actor           VARCHAR(80) NOT NULL,
    detail          TEXT NOT NULL DEFAULT '{}',
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_order_rocket ON work_order(rocket_id);
CREATE INDEX idx_operation_work_order ON operation(work_order_id);
CREATE INDEX idx_schedule_assignment_version ON schedule_assignment(schedule_version_id);
CREATE INDEX idx_schedule_assignment_resource_time ON schedule_assignment(resource_id, start_at, end_at);
CREATE INDEX idx_audit_event_entity ON audit_event(entity_type, entity_id);
CREATE INDEX idx_proposal_status ON schedule_proposal(status);
CREATE INDEX idx_test_result_work_order ON test_result(work_order_id);
CREATE INDEX idx_launch_preparation_rocket ON launch_preparation(rocket_id);
