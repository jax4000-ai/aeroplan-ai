# agent-service

Python + FastAPI + LangGraph. Implements the 5-agent disruption-analysis workflow: 1 supervisor
+ 4 specialists (Scheduling Optimization, Materials Readiness, Bottleneck & Risk, Schedule
Recovery), backed by Amazon Bedrock for reasoning. **The agents explain and compare
already-computed candidate schedules; they never invent start times, resource assignments, or
feasibility.** That hard rule must be enforced in code (schema validation + rejecting any
"recommended candidate" that isn't literally one of the candidate IDs it was given), not just in
a prompt.

**This file is your scope.** Do not edit `../scheduling-service`, `../manufacturing-event-service`,
`../../frontend`, or `../../infrastructure`. If you need a new read-only endpoint on
`scheduling-service` for a tool call, write the need down (e.g. in this README's "needs from
scheduling-service" section below) rather than implementing it yourself.

## Source of truth

1. `../../docs/api-specification.md` — the "agent-service" section: exact request/response
   shape for `POST /agents/analyze-disruption`. This is a frozen contract — Step Functions and
   `scheduling-service` both depend on this exact shape.
2. `../../docs/architecture-decisions.md` — ADR-003 (why LangGraph + Bedrock, why Agents
   Classic is avoided, the safety rule).
3. `../../docs/infrastructure-spec.md` — env vars `BEDROCK_MODEL_ID` (default
   `anthropic.claude-haiku-4-5-20251001-v1:0`), `SCHEDULING_SERVICE_BASE_URL`, `AWS_REGION`.
   Bedrock access for Claude/Nova models is already enabled in this AWS account in `us-east-2` —
   no model-access request needed.

## Needs from scheduling-service (coordinate via the main session, don't implement yourself)

- Read-only resource/work-order lookups by ID (for turning IDs into human-readable names in the
  summary text) — likely already covered by `GET /api/work-orders/{id}` and a resources list
  endpoint; flag here if something's missing once you're implementing.

## P0 checklist

1. `uv`-managed Python project (`uv init`, `pyproject.toml`), Python 3.12+. Dependencies:
   `fastapi`, `uvicorn`, `langgraph`, `langchain-aws` (or `boto3` directly via the Bedrock
   Converse API — your choice, document which), `pydantic` v2 for strict schema validation,
   `httpx` (for the scheduling-service tool calls), `pytest` + `pytest-asyncio` for tests.
2. Define Pydantic models for the exact request/response shape in `api-specification.md` — these
   are your schema-validation enforcement point. The LangGraph graph's final node output must
   validate against the response model or the endpoint returns a structured error (never a
   malformed 200).
3. Build the LangGraph graph:
   - **Supervisor** node: receives the disruption + candidate schedules + material shortages,
     decides which specialists to run (can be "all 4" unconditionally for P0 — routing logic is
     a nice-to-have, not required), and at the end combines their structured outputs into the
     final response + a human-readable `summary`.
   - **Scheduling Optimization Agent**: given the candidate schedules it was handed (not
     computed by this agent), produce a tradeoff explanation per candidate (late hours,
     overtime, resource changes) in plain language.
   - **Materials Readiness Agent**: given `materialShortages`, identify which candidate(s) are
     blocked vs. feasible given current material availability.
   - **Bottleneck & Risk Agent**: given the downtime event + candidates, estimate lateness risk
     per affected work order using simple rules (e.g. slack time = due date − candidate finish
     time) — present this as a rule-based calculation the LLM narrates, not an LLM-invented
     number.
   - **Schedule Recovery Agent**: synthesizes the above into
     `recommendedCandidateIndex` (must be a valid index into the given `candidateSchedules`
     array, or `null` with an `error` field if none are acceptable) + `tradeoffs` text.
4. Validate Bedrock output against your Pydantic schema; on a parse/validation failure, retry
   once with a corrective prompt, then fall back to `recommendedCandidateIndex: null` with an
   `error` field rather than guessing.
5. `GET /health`.
6. Tests: mock the Bedrock client, assert the graph produces schema-valid output for a happy
   path, assert an intentionally malformed model response is caught and produces the documented
   fallback (never a crash, never an invented candidate index).
7. `Dockerfile` (python slim base), expose port `8000`.

## Definition of done for P0

`uv run pytest` passes (with Bedrock mocked). `POST /agents/analyze-disruption` with a realistic
payload (matching the request shape) returns a response that validates against the documented
schema, including a human-readable `summary` a planner could actually read and act on.
