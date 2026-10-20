# API specification (frozen contract — v1)

All responses are JSON. Errors follow the RFC 9457 Problem Details shape (same convention as
the companion Mission Notes project):

```json
{
  "type": "https://aeroplan.example/problems/{status}",
  "title": "Human-readable title",
  "status": 409,
  "detail": "Specific reason",
  "errorCode": "SCHEDULE_PROPOSAL_ALREADY_DECIDED",
  "requestId": "..."
}
```

## scheduling-service (Java / Spring Boot) — public, behind API Gateway

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/work-orders` | List/search/filter work orders (`?status=`, `?priority=`, `?query=`) |
| `POST` | `/api/work-orders` | Create a work order + its operations/dependencies |
| `GET` | `/api/work-orders/{id}` | Read one work order with operations |
| `PUT` | `/api/work-orders/{id}` | Update; rejects if dependency graph becomes cyclic |
| `GET` | `/api/schedules/current` | The currently published `ScheduleVersion` + assignments |
| `POST` | `/api/schedules/optimize` | Run the scheduling engine; returns 1–3 candidate `DRAFT` versions, does not publish |
| `GET` | `/api/schedules/{versionId}` | Read one schedule version (for comparison) |
| `GET` | `/api/materials/shortages` | Read-only: materials with `on_hand_quantity` insufficient for near-term reserved operations |
| `POST` | `/api/disruptions` | Record a disruption directly via API (bypasses SQS, useful for demo/manual testing) |
| `GET` | `/api/proposals` | List schedule proposals (`?status=PENDING`) |
| `GET` | `/api/proposals/{id}` | Full proposal: agent findings + candidate schedule comparison |
| `POST` | `/api/proposals/{id}/approve` | Approve; publishes candidate version, sends `SendTaskSuccess` to Step Functions, publishes SNS |
| `POST` | `/api/proposals/{id}/reject` | Reject; sends `SendTaskSuccess` with `{"decision":"REJECTED"}` payload |
| `GET` | `/api/audit-events` | Paginated audit trail (`?entityType=`, `?entityId=`) |
| `GET` | `/actuator/health` | Health check (ECS/ALB target group + API Gateway) |
| `POST` | `/internal/proposals` | **Internal only**, called by the Step Functions HTTP-bridge Lambda via its `waitForTaskToken` task: body includes `taskToken`, `executionArn`, `machineDowntimeId`, `basedOnScheduleVersionId`, `candidateSchedules`, `agentFindings`. Persists a `PENDING` proposal with the task token stored (Postgres + DynamoDB `WorkflowState`) and returns `{ "proposalId": 7 }` immediately — it does **not** resolve the Step Functions wait; only a later `/approve` or `/reject` call does that. |
| `POST` | `/internal/proposals/expire` | **Internal only**, called on Step Functions `States.Timeout` (72h default): marks any still-`PENDING` proposal for that `executionArn` as `EXPIRED` |

Both `/approve` and `/reject` perform their business transaction (publish or mark-rejected,
write the audit event, publish SNS on approval) **synchronously in the same request**, then call
`SendTaskSuccess`/`SendTaskFailure` against the stored task token purely to close out the Step
Functions execution history — the API call is the authoritative action, not the callback. This
requires `scheduling-service`'s ECS task role to include `states:SendTaskSuccess` and
`states:SendTaskFailure` (see `infrastructure-spec.md`).

### Key request/response shapes

`POST /api/work-orders`:
```json
{
  "partNumber": "WO-101",
  "title": "Engine housing",
  "priority": "CRITICAL",
  "dueAt": "2026-11-01T00:00:00Z",
  "operations": [
    { "name": "Engine housing machining", "durationMinutes": 240, "requiredSkill": "MACHINIST",
      "requiredResourceType": "CNC_STATION", "sequenceIndex": 0, "dependsOnSequenceIndex": null }
  ]
}
```

`POST /api/schedules/optimize` request:
```json
{ "reason": "MACHINE_DOWNTIME", "unavailableResourceIds": [7], "unavailableUntil": "2026-10-17T12:00:00Z" }
```
response (array of candidates, each including a feasibility report):
```json
[
  {
    "scheduleVersionId": 42,
    "objectiveScore": 18.5,
    "lateWorkOrders": [{"workOrderId": 104, "lateHours": 3}],
    "overtimeMinutesUsed": 120,
    "changedAssignments": 6,
    "assignments": [ { "operationId": 501, "resourceId": 9, "startAt": "...", "endAt": "..." } ]
  }
]
```

`GET /api/proposals/{id}`:
```json
{
  "id": 7,
  "status": "PENDING",
  "machineDowntime": { "eventId": "evt-9001", "resourceId": 7, "reason": "Preventive maintenance" },
  "basedOnScheduleVersionId": 41,
  "candidateScheduleVersionId": 42,
  "agentSummary": "Recommend Candidate A: protects WO-101 and WO-104 critical deadlines at the cost of 2 hours overtime on Turbopump Station.",
  "agentFindings": {
    "schedulingOptimization": { "candidates": [ /* same shape as /optimize response */ ] },
    "materialsReadiness": { "blockedOperations": [], "availableNow": [501, 502] },
    "bottleneckRisk": { "atRiskWorkOrders": [{"workOrderId": 104, "lateProbability": 0.7}] },
    "recovery": { "recommendedCandidateIndex": 0, "tradeoffs": "..." }
  }
}
```

`POST /api/proposals/{id}/approve` request: `{ "approvedBy": "planner.ada" }`
- `409 Conflict` (`errorCode: PROPOSAL_ALREADY_DECIDED`) if status is no longer `PENDING`
  (optimistic concurrency — two planners racing to decide the same proposal).

## manufacturing-event-service (Java / Spring Boot) — public (thin) + SQS consumer

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/downtime-events` | Alternate HTTP ingestion path for the downtime simulator (in addition to SQS) |
| `GET` | `/actuator/health` | Health check |

SQS consumer (not HTTP): listens to `manufacturing-downtime-events`, handles the JSON shape in
`architecture.md`'s example event. On each message:
1. Check `IdempotencyRecords[eventId]` in DynamoDB — if `COMPLETED`, ack and skip.
2. Insert `machine_downtime` row (unique constraint on `event_id` as a second guard).
3. Call Step Functions `StartExecution` with the event as input.
4. Mark idempotency record `COMPLETED`.
5. On any failure before step 4, let the message become visible again (SQS redelivery); after
   5 attempts it routes to the DLQ automatically (`maxReceiveCount: 5`).

## agent-service (Python / FastAPI / LangGraph) — **internal only**, not exposed via API Gateway

Called only by Step Functions (via a VPC Link / internal ALB listener) and, for local dev, the
`scheduling-service`'s own test harness.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/agents/analyze-disruption` | Run supervisor + 4 specialists, return structured findings |
| `GET` | `/health` | Health check |

Request:
```json
{
  "downtimeEvent": { "eventId": "evt-9001", "resourceId": 7, "reason": "Preventive maintenance",
                     "estimatedRecoveryAt": "2026-10-17T12:00:00Z" },
  "candidateSchedules": [ /* same shape scheduling-service returns from /optimize */ ],
  "materialShortages": [ /* same shape as GET /api/materials/shortages */ ],
  "currentScheduleVersionId": 41
}
```
`candidateSchedules` and `materialShortages` are supplied by the Step Functions
`ParallelImpactChecks` state (see `workflows/step-functions/disruption-recovery.asl.json`) — the
agent service never calls `/api/schedules/optimize` or `/api/materials/shortages` itself for
this flow; it only uses its scheduling-service "tools" for supplementary read-only lookups
(e.g. resource names, work-order titles) needed to write a readable summary.

Response — **must** match the `agentFindings` shape embedded in `GET /api/proposals/{id}`
above exactly (`schedulingOptimization`, `materialsReadiness`, `bottleneckRisk`, `recovery`,
plus a top-level `summary` string). The agent service's tools call back into
`scheduling-service`'s read-only endpoints (`GET /api/work-orders`, `GET /api/schedules/{id}`,
material/resource lookups — add read endpoints as needed) to fetch ground truth; **the LLM must
never invent start times, resource assignments, or material quantities.** If the model's JSON
output fails schema validation or proposes an infeasible assignment, the agent-service returns
`agentFindings.recovery.recommendedCandidateIndex: null` with an `error` field, and
`scheduling-service` surfaces that to the planner rather than silently picking something.

## Events

### SQS message — machine downtime (matches `architecture.md`)
```json
{
  "eventId": "evt-9001",
  "eventType": "MACHINE_DOWNTIME",
  "resourceId": "ENGINE-STATION-01",
  "startedAt": "2026-10-17T09:00:00Z",
  "estimatedRecoveryAt": "2026-10-17T12:00:00Z",
  "reason": "Preventive maintenance"
}
```
`resourceId` here is the human-readable resource name; `manufacturing-event-service` resolves
it to `resource.id` on ingestion (seed data must keep names unique).

### SNS notification — schedule published
```json
{ "scheduleVersionId": 42, "publishedAt": "...", "approvedBy": "planner.ada",
  "affectedWorkOrderIds": [101, 102, 104], "summary": "..." }
```

## GraphQL (P1 — added after the REST core works)

A single query-only GraphQL endpoint (`/graphql` on `scheduling-service`, via `graphql-java` or
`spring-graphql`) exposing `workOrders`, `resources`, and `scheduleVersion(id)` as a more
flexible read path for the dashboard (e.g. fetch only the fields a given screen needs). Mutations
stay on REST — this mirrors a common, defensible real-world pattern (GraphQL for flexible reads,
REST for well-defined commands) and is easy to explain in an interview.
