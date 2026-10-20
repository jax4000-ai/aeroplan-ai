# Architecture decisions

## ADR-001: Postgres (RDS) for system of record, DynamoDB for two narrow concerns

**Decision:** All manufacturing domain data (work orders, operations, dependencies, resources,
schedule versions, proposals, audit) lives in PostgreSQL. DynamoDB holds only
`IdempotencyRecords` and `WorkflowState`.

**Why:** The domain is deeply relational — dependency graphs, resource conflicts, optimistic
locking, joins for the Gantt chart and audit trail. Forcing this into DynamoDB would mean
re-implementing joins and transactions in application code for no benefit. Conversely,
idempotency keys and workflow-callback metadata are pure key-value lookups with no query
patterns beyond "get by ID" and a TTL — DynamoDB's single-digit-millisecond reads and
pay-per-request pricing fit perfectly, and demonstrate *choosing* the right tool rather than
defaulting to one database everywhere.

**Tradeoff accepted:** `schedule_proposal.step_functions_task_token` is denormalized into both
Postgres and DynamoDB's `WorkflowState`. This is deliberate — Postgres is the source of truth
for "what did the planner see and decide," DynamoDB is the fast lookup Step Functions-adjacent
code paths use. A reconciliation job would be needed in a real production system; noted in
"what I'd change with more time."

## ADR-002: Step Functions (Standard) for disruption recovery orchestration

**Decision:** Use a Standard (not Express) Step Functions workflow with native `Parallel` states
and `waitForTaskToken` for human approval.

**Why:** Standard workflows support executions up to a year and are built for exactly this
"pause for a human, resume later" pattern via task tokens — no polling, no extra Lambda to
check "has someone approved yet." Parallel branches give a clean way to run schedule
re-optimization and agent analysis concurrently and join their results, with Step Functions
handling partial failures and retries declaratively instead of in application code.

**Tradeoff accepted:** Standard workflows cost per state transition (small, but non-zero) and
have slightly higher latency than Express. For a human-approval workflow measured in minutes
to hours, this is irrelevant.

## ADR-003: LangGraph + Amazon Bedrock for the agent graph; deterministic engine stays authoritative

**Decision:** Five agents (1 supervisor + 4 specialists) implemented as a LangGraph graph in
Python, calling Amazon Bedrock (Claude/Nova models — already available in this account in
`us-east-2`, confirmed before building) for reasoning and explanation. The agents call back into
`scheduling-service`'s read-only endpoints as tools for ground truth; they do not compute
schedules themselves.

**Why:** Amazon Bedrock Agents "Classic" is closed to new customers as of July 30, 2026.
LangGraph gives full control over the multi-agent graph (supervisor routing, specialist fan-out,
structured output validation) without depending on a managed agent product that's being
deprecated for new usage. Using Bedrock (vs. a third-party LLM API) keeps the AI workload inside
the same AWS account/network boundary as everything else, which matters for the "AWS-native"
story of this project.

**Hard safety rule, enforced in code, not just prompted:** the deterministic scheduling engine
computes feasible candidate schedules first; the agent service's output is schema-validated, and
any candidate the agents recommend that doesn't match one of the engine's own candidate IDs is
rejected before it ever reaches a planner. The LLM explains and ranks; it cannot invent.

## ADR-004: Cost-conscious AWS footprint for a demo/portfolio deployment

**Decision:** Reuse the account's existing default VPC and its public subnets (no new NAT
Gateway); Fargate tasks get public IPs with locked-down security groups instead of private
subnets + NAT; RDS is single-AZ `db.t4g.micro`; DynamoDB tables are on-demand billing; Step
Functions/SQS/SNS are inherently pay-per-use.

**Why:** This is a personal portfolio/interview project, not a production system with an SLA. A
NAT Gateway alone (~$32/month plus data processing) would be the single largest line item and
buys nothing a demo needs. Documenting this tradeoff *as a tradeoff* — rather than silently
building something that looks production-grade but would surprise someone with a bill — is
itself the kind of operational judgment worth demonstrating.

**Production upgrade path (explicitly not built, but designed for):** move Fargate tasks to
private subnets behind a NAT Gateway or VPC endpoints, RDS Multi-AZ with a read replica,
Cognito-authenticated API Gateway, WAF, per-environment Terraform workspaces, and a
staging/production promotion pipeline.

## ADR-005: Java modular-monolith-first for the two Java services

**Decision:** `scheduling-service` and `manufacturing-event-service` are separate Spring Boot
applications (separate deployables, separate ECR images, separate ECS services) sharing one
RDS instance, rather than one monolith or many fine-grained microservices.

**Why:** The source planning document for this project explicitly recommends starting modular
and splitting only once there's a real reason — SQS consumption and HTTP API serving have
different scaling/failure characteristics (a slow downstream call blocking the event consumer
shouldn't affect `GET /api/work-orders` latency), which is reason enough to split exactly these
two, while keeping everything else (work order logic, scheduling engine, proposal handling) in
one service rather than over-fragmenting.

## ADR-006 (frontend): No charting library for the Gantt view

**Decision:** Build the Gantt chart as custom SVG/CSS driven directly by
`schedule_assignment` rows, not a third-party Gantt/charting dependency.

**Why:** The data shape is simple (start/end per resource row); a hand-built component avoids a
heavy dependency, licensing questions, and keeps full control over showing proposal-comparison
overlays (current vs. candidate) which most off-the-shelf Gantt libraries don't support without
fighting their API. Mirrors the "no new chart dependency" precedent from the companion Mission
Notes project's operations dashboard.
