# API specification (frozen contract — v1)

All responses are JSON. Errors follow the RFC 9457 Problem Details shape (same convention as
the companion Mission Notes project):

```json
{
  "type": "https://aeroplan.example/problems/{status}",
  "title": "Human-readable title",
  "status": 409,
  "detail": "Specific reason",
  "errorCode": "PROPOSAL_ALREADY_DECIDED",
  "requestId": "..."
}
```

Every public endpoint below is reachable through API Gateway at the same base URL
(`VITE_API_URL`); API Gateway routes by path prefix to the owning service's internal ALB target
group (see `infrastructure-spec.md`). The table below groups endpoints by **owning service** —
that is which service's workstream implements and is authoritative for that endpoint — not by
literal path prefix (several services share the `/api/...` namespace).

## work-order-service — public, behind API Gateway

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/rockets` | List rockets/missions (`?status=`) with basic progress summary |
| `GET` | `/api/rockets/{id}` | One rocket with its work orders rolled up |
| `GET` | `/api/work-orders` | Search and filter work orders (`?rocketId=`, `?status=`, `?priority=`, `?workType=`, `?query=`) |
| `POST` | `/api/work-orders` | Create a work order + its operations/dependencies |
| `GET` | `/api/work-orders/{id}` | Read one work order with operations |
| `PUT` | `/api/work-orders/{id}` | Update; rejects if dependency graph becomes cyclic |
| `GET` | `/api/resources` | List resources and current availability (`?resourceType=`, `?availabilityStatus=`) |
| `GET` | `/api/materials` | List materials/BOM status (`?status=LOW` etc.) |
| `GET` | `/api/materials/shortages` | Read-only: materials with `quantity_on_hand` insufficient for near-term reserved operations |
| `GET` | `/api/audit-events` | Paginated audit trail (`?entityType=`, `?entityId=`) — append-only table is shared, but reads are served here since this service owns the baseline schema |
| `GET` | `/actuator/health` | Health check (ECS/ALB target group + API Gateway) |
| `GET` | `/graphql` | Query-only GraphQL endpoint (P1): `workOrders`, `rockets`, `resources`, `auditEvents` — see "GraphQL" section below |

## scheduling-service — public, behind API Gateway

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/schedules/current` | The currently published `ScheduleVersion` + assignments |
| `POST` | `/api/schedules/optimize` | Run the scheduling engine; returns 1–3 candidate `DRAFT` versions, does not publish |
| `GET` | `/api/schedules/{versionId}` | Read one schedule version (for comparison) |
| `GET` | `/api/proposals` | List schedule proposals (`?status=PENDING`) |
| `GET` | `/api/proposals/{id}` | Full proposal: agent findings + candidate schedule comparison |
| `POST` | `/api/proposals/{id}/approve` | Approve; publishes candidate version, sends `SendTaskSuccess` to Step Functions, triggers `notification-service` |
| `POST` | `/api/proposals/{id}/reject` | Reject; sends `SendTaskSuccess` with `{"decision":"REJECTED"}` payload |
| `GET` | `/api/launch-readiness` | Latest `LaunchPreparation` rollup per rocket (`?rocketId=`) |
| `POST` | `/api/schedules/what-if` | (P1/P2) Run the engine against a hypothetical change without persisting anything, for scenario exploration |
| `GET` | `/actuator/health` | Health check |
| `POST` | `/internal/proposals` | **Internal only**, called by the Step Functions HTTP-bridge Lambda via its `waitForTaskToken` task: body includes `taskToken`, `executionArn`, `machineDowntimeId`, `basedOnScheduleVersionId`, `candidateSchedules`, `agentFindings`. Persists a `PENDING` proposal with the task token stored (Postgres + DynamoDB `WorkflowState`) and returns `{ "proposalId": 7 }` immediately — it does **not** resolve the Step Functions wait; only a later `/approve` or `/reject` call does that. |
| `POST` | `/internal/proposals/expire` | **Internal only**, called on Step Functions `States.Timeout` (72h default): marks any still-`PENDING` proposal for that `executionArn` as `EXPIRED` |

Both `/approve` and `/reject` perform their business transaction (publish or mark-rejected,
write the audit event) **synchronously in the same request**, then call
`SendTaskSuccess`/`SendTaskFailure` against the stored task token purely to close out the Step
Functions execution history — the API call is the authoritative action, not the callback. This
requires `scheduling-service`'s ECS task role to include `states:SendTaskSuccess` and
`states:SendTaskFailure` (see `infrastructure-spec.md`). On approval, `scheduling-service` also
calls `notification-service`'s internal endpoint (or publishes directly to SNS — either is
acceptable; document which you chose) rather than publishing to SNS itself, to keep notification
delivery logic in one place.

### Key request/response shapes

`POST /api/work-orders`:
```json
{
  "rocketId": 1,
  "partNumber": "WO-101",
  "title": "Engine housing",
  "workType": "MANUFACTURING",
  "priority": "CRITICAL",
  "dueAt": "2026-11-01T00:00:00Z",
  "operations": [
    { "name": "Engine housing machining", "durationMinutes": 240, "requiredSkill": "MACHINIST",
      "requiredResourceType": "ASSEMBLY_STATION", "sequenceIndex": 0, "dependsOnSequenceIndex": null }
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
    "launchReadiness": { "affectedRocketIds": [1], "readinessImpact": "NONE" },
    "recovery": { "recommendedCandidateIndex": 0, "tradeoffs": "..." }
  }
}
```

`GET /api/launch-readiness?rocketId=1`:
```json
{
  "rocketId": 1,
  "readinessStatus": "AT_RISK",
  "readinessScore": 78.5,
  "weatherStatus": "GO",
  "plannedLaunchAt": "2026-11-15T14:00:00Z",
  "notes": "WO-104 (critical) at risk of 3-hour delay pending ENGINE-STATION-01 recovery."
}
```

`POST /api/proposals/{id}/approve` request: `{ "approvedBy": "planner.ada" }`
- `409 Conflict` (`errorCode: PROPOSAL_ALREADY_DECIDED`) if status is no longer `PENDING`
  (optimistic concurrency — two planners racing to decide the same proposal).

## event-processing-service — public (thin) + SQS consumer

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/disruptions` | Report downtime or material delay (HTTP alternative to SQS; also used for demo/manual testing) |
| `POST` | `/api/test-results` | Record a test/integration/quality result (HTTP alternative to SQS) |
| `GET` | `/api/test-results` | Query recorded test results (`?workOrderId=`, `?result=`) |
| `GET` | `/actuator/health` | Health check |

SQS consumer (not HTTP): listens to `manufacturing-events`, handling two event types sharing one
queue (`eventType` discriminates — see shapes below). On each message:
1. Check `IdempotencyRecords[eventId]` in DynamoDB — if `COMPLETED`, ack and skip. This is the
   "Validate Event" step in the architecture diagram's agent workflow.
2. Insert `machine_downtime` or `test_result` row (unique constraint on `event_id` as a second
   guard). This, plus the read-only lookup of affected work orders via dependency joins, is the
   "Analyze Impact" step.
3. Call Step Functions `StartExecution` with the event as input (only for `MACHINE_DOWNTIME`
   events for P0 — a failing `TEST_RESULT` can also trigger recovery analysis as a P1 extension;
   document whichever you implement).
4. Mark the idempotency record `COMPLETED`.
5. On any failure before step 4, let the message become visible again (SQS redelivery); after 5
   attempts it routes to the DLQ automatically (`maxReceiveCount: 5`).

## notification-service — mostly internal

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/internal/notifications` | Called by `scheduling-service` on proposal approval/rejection and by `event-processing-service` on new disruptions; publishes to SNS and/or simulates Slack/email delivery (log-based simulation is fine for a demo — document clearly that this is not integrated with real Slack/SMTP unless you choose to add it) |
| `GET` | `/api/notifications` | List recently sent notifications, for the dashboard to show "who was told what" (P1) |
| `GET` | `/actuator/health` | Health check |

## agent-service (Python / FastAPI / LangGraph) — **internal only**, not exposed via API Gateway

Called only by Step Functions (via a VPC Link / internal ALB listener) and, for local dev, the
`scheduling-service`'s own test harness.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/agents/analyze-disruption` | Run supervisor + 4 specialists (Scheduling Optimization, Materials Readiness, Bottleneck & Risk, Launch Readiness), then the Schedule Recovery Agent synthesizes a recommendation. Returns structured findings. |
| `GET` | `/health` | Health check |

Request:
```json
{
  "downtimeEvent": { "eventId": "evt-9001", "resourceId": 7, "reason": "Preventive maintenance",
                     "estimatedRecoveryAt": "2026-10-17T12:00:00Z" },
  "candidateSchedules": [ /* same shape scheduling-service returns from /optimize */ ],
  "materialShortages": [ /* same shape as GET /api/materials/shortages */ ],
  "affectedRocketIds": [1],
  "currentScheduleVersionId": 41
}
```
`candidateSchedules`, `materialShortages`, and `affectedRocketIds` are supplied by the Step
Functions workflow (see `workflows/step-functions/disruption-recovery.asl.json`) — the agent
service never calls `/api/schedules/optimize` or `/api/materials/shortages` itself for this
flow; it only uses its scheduling-service "tools" for supplementary read-only lookups (e.g.
resource names, work-order titles, rocket launch windows for the Launch Readiness Agent) needed
to write a readable summary.

Response — **must** match the `agentFindings` shape embedded in `GET /api/proposals/{id}` above
exactly (`schedulingOptimization`, `materialsReadiness`, `bottleneckRisk`, `launchReadiness`,
`recovery`, plus a top-level `summary` string). The agent service's tools call back into
`work-order-service`'s read-only endpoints to fetch ground truth; **the LLM must never invent
start times, resource assignments, material quantities, or launch dates.** If the model's JSON
output fails schema validation or proposes an infeasible assignment, the agent-service returns
`agentFindings.recovery.recommendedCandidateIndex: null` with an `error` field, and
`scheduling-service` surfaces that to the planner rather than silently picking something.

## Events (SQS, one shared queue `manufacturing-events`)

### Machine downtime
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

### Test result
```json
{
  "eventId": "evt-9050",
  "eventType": "TEST_RESULT",
  "workOrderId": 105,
  "testType": "ENGINE_HOT_FIRE",
  "result": "FAIL",
  "performedAt": "2026-10-17T09:00:00Z"
}
```
`resourceId` in the downtime event is the human-readable resource name;
`event-processing-service` resolves it to `resource.id` on ingestion (seed data must keep names
unique).

### SNS notification — schedule published (published by `notification-service`)
```json
{ "scheduleVersionId": 42, "publishedAt": "...", "approvedBy": "planner.ada",
  "affectedWorkOrderIds": [101, 102, 104], "affectedRocketIds": [1], "summary": "..." }
```

## GraphQL (P1 — added after the REST core works)

A single query-only GraphQL endpoint (`/graphql` on `work-order-service`, via `graphql-java` or
`spring-graphql`) exposing `workOrders`, `rockets`, `resources`, and `auditEvents` as a more
flexible read path for the dashboard (e.g. fetch only the fields a given screen needs).
`scheduleVersion(id)` is also exposed here as a federated-style passthrough read to
`scheduling-service` (simple HTTP call inside the resolver — not a full GraphQL federation setup,
which would be over-engineering for this project's scope). Mutations stay on REST — this mirrors
a common, defensible real-world pattern (GraphQL for flexible reads, REST for well-defined
commands) and is easy to explain in an interview.
