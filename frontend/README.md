# frontend

React 19 + TypeScript + Vite (match the companion Mission Notes project's toolchain/version
choices for consistency unless there's a good reason to deviate). Four screens, all reading from
`scheduling-service`'s REST API (and later GraphQL, P1) through API Gateway.

**This file is your scope.** Do not edit `../services/*` or `../infrastructure` — if the API
doesn't yet return something you need, build against a documented mock/fixture matching
`api-specification.md`'s shapes and flag the gap, rather than guessing a different shape.

## Source of truth

1. `../docs/api-specification.md` — every request/response shape you'll render. Treat it as
   frozen; if `scheduling-service` isn't finished yet, build a local fixture/mock server (e.g.
   MSW or a tiny JSON file) matching these exact shapes so you can develop independently, then
   swap to the real API via `VITE_API_URL`.
2. `../docs/architecture-decisions.md` — ADR-006 (no charting library for the Gantt view — build
   it with SVG/CSS).
3. `../docs/infrastructure-spec.md` — `VITE_API_URL` env var.

## The four screens (P0)

1. **Production schedule** — work-order list (status, priority, due date, filter/search) +
   a Gantt view of the current published `ScheduleVersion`'s `schedule_assignment` rows (one row
   per resource, bars positioned/sized from `start_at`/`end_at`, colored by work-order priority).
2. **Factory health** — overdue/at-risk work orders, resources currently `DOWN`, a simple
   bottleneck callout (reuse whatever `bottleneckRisk` data is available from the most recent
   proposal, or compute a basic "operations waiting on a DOWN resource" view directly from
   `/api/work-orders` + `/api/schedules/current` if no proposal exists yet).
3. **AI recovery proposals** — list `PENDING` proposals; detail view shows the agent `summary`,
   each specialist's findings, and a **side-by-side comparison** of the current published
   schedule vs. the candidate (which work orders become late/on-time, overtime delta) with
   **Approve** / **Reject** buttons calling the respective endpoints.
4. **Audit & event history** — paginated table from `/api/audit-events`, filterable by entity
   type, with enough detail to reconstruct "what happened" for a given disruption end-to-end
   (downtime received → proposal created → agent findings → decision → published version).

## P0 checklist

1. Vite + React + TypeScript scaffold, same lint/build tooling pattern as the companion project
   (`oxlint`, `tsc -b && vite build`).
2. A typed `api.ts` client matching `api-specification.md`'s shapes exactly (hand-written types
   are fine; OpenAPI codegen is a nice-to-have, not required for P0).
3. Build all 4 screens against the documented shapes (using mock data/fixtures if the real
   backend isn't deployed yet — this frontend workstream should **not** block on the Java/Python
   services being finished).
4. Responsive layout, loading/error/empty states on every screen (mirror the quality bar of the
   companion Mission Notes dashboard — demo notices, clear empty states, no unhandled promise
   rejections shown to the user as blank screens).
5. `npm run lint` and `npm run build` both clean.
6. A short `README.md` update (this file) noting any shape mismatches you had to work around.

## Definition of done for P0

All four screens render real data once pointed at a running `scheduling-service` (local or
deployed) via `VITE_API_URL`; approving/rejecting a proposal on screen 3 is reflected
immediately (poll or refetch) on screens 1 and 4.
