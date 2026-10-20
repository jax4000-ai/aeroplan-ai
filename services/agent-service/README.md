# agent-service

Python + FastAPI + LangGraph. Implements the 6-agent disruption-analysis workflow: 1 supervisor
+ 5 specialists (Scheduling Optimization, Materials Readiness, Bottleneck & Risk, **Launch
Readiness**, Schedule Recovery), backed by Amazon Bedrock for reasoning. **The agents explain
and compare already-computed candidate schedules; they never invent start times, resource
assignments, material quantities, or launch dates.** That hard rule must be enforced in code
(schema validation + rejecting any "recommended candidate" that isn't literally one of the
candidate IDs it was given), not just in a prompt.

**This file is your scope.** Do not edit `../work-order-service`, `../scheduling-service`,
`../event-processing-service`, `../notification-service`, `../../frontend`, or
`../../infrastructure`. If you need a new read-only endpoint on `work-order-service` for a tool
call, write the need down (in this README's "needs" section below) rather than implementing it
yourself.

## Source of truth

1. `../../docs/api-specification.md` — the "agent-service" section: exact request/response
   shape for `POST /agents/analyze-disruption`. This is a frozen contract — Step Functions and
   `scheduling-service` both depend on this exact shape.
2. `../../docs/architecture-decisions.md` — ADR-003 (why LangGraph + Bedrock, why Agents
   Classic is avoided, the safety rule, why the Launch Readiness Agent exists).
3. `../../docs/infrastructure-spec.md` — env vars `BEDROCK_MODEL_ID` (default
   `anthropic.claude-haiku-4-5-20251001-v1:0`), `WORK_ORDER_SERVICE_BASE_URL`, `AWS_REGION`.
   Bedrock access for Claude/Nova models is already enabled in this AWS account in `us-east-2` —
   no model-access request needed.

## Needs from other services (coordinate via the main session, don't implement yourself)

- Read-only rocket/work-order/resource lookups by ID (for turning IDs into human-readable names
  and launch-window dates in the summary text and for the Launch Readiness Agent) — likely
  covered by `work-order-service`'s `GET /api/rockets/{id}` and `GET /api/work-orders/{id}`; flag
  here if something's missing once you're implementing.

## P0 checklist

1. `uv`-managed Python project (`uv init`, `pyproject.toml`), Python 3.12+. Dependencies:
   `fastapi`, `uvicorn`, `langgraph`, `langchain-aws` (or `boto3` directly via the Bedrock
   Converse API — your choice, document which), `pydantic` v2 for strict schema validation,
   `httpx` (for the work-order-service tool calls), `pytest` + `pytest-asyncio` for tests.
2. Define Pydantic models for the exact request/response shape in `api-specification.md` — these
   are your schema-validation enforcement point. The LangGraph graph's final node output must
   validate against the response model or the endpoint returns a structured error (never a
   malformed 200).
3. Build the LangGraph graph:
   - **Supervisor** node: receives the disruption + candidate schedules + material shortages +
     affected rocket IDs, decides which specialists to run (can be "all 5" unconditionally for
     P0 — routing logic is a nice-to-have, not required), and at the end combines their
     structured outputs into the final response + a human-readable `summary`.
   - **Scheduling Optimization Agent**: given the candidate schedules it was handed (not
     computed by this agent), produce a tradeoff explanation per candidate (late hours,
     overtime, resource changes) in plain language.
   - **Materials Readiness Agent**: given `materialShortages`, identify which candidate(s) are
     blocked vs. feasible given current material availability.
   - **Bottleneck & Risk Agent**: given the downtime event + candidates, estimate lateness risk
     per affected work order using simple rules (e.g. slack time = due date − candidate finish
     time) — present this as a rule-based calculation the LLM narrates, not an LLM-invented
     number.
   - **Launch Readiness Agent**: given `affectedRocketIds` and the candidates, assess whether
     this disruption threatens any rocket's launch window (e.g. a candidate that pushes a
     critical work order's finish time past a rocket's `launch_window_start` is flagged as
     `readinessImpact: "AT_RISK"` or `"BLOCKED"`; otherwise `"NONE"`). This agent is what makes
     the project's "launch readiness" story real rather than a static dashboard label.
   - **Schedule Recovery Agent**: synthesizes all of the above into
     `recommendedCandidateIndex` (must be a valid index into the given `candidateSchedules`
     array, or `null` with an `error` field if none are acceptable) + `tradeoffs` text.
4. Validate Bedrock output against your Pydantic schema; on a parse/validation failure, retry
   once with a corrective prompt, then fall back to `recommendedCandidateIndex: null` with an
   `error` field rather than guessing.
5. `GET /health`.
6. Tests: mock the Bedrock client, assert the graph produces schema-valid output for a happy
   path (including a non-trivial `launchReadiness` result), assert an intentionally malformed
   model response is caught and produces the documented fallback (never a crash, never an
   invented candidate index).
7. `Dockerfile` (python slim base), expose port `8000`.

## Definition of done for P0

`uv run pytest` passes (with Bedrock mocked). `POST /agents/analyze-disruption` with a realistic
payload (matching the request shape, including a rocket with a near-term launch window) returns
a response that validates against the documented schema, including a human-readable `summary`
and a meaningful `launchReadiness` assessment a planner could actually read and act on.
