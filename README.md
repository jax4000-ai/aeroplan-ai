# AeroPlan AI

Aerospace manufacturing, assembly, integration, test, and launch scheduling platform, with a
multi-agent AI workflow that proposes recovery plans when disruptions happen — subject to
mandatory human approval before anything is published.

> **Independent portfolio/demo project.** Synthetic data only. Not affiliated with Blue Origin
> or any employer; built to demonstrate backend engineering, distributed-systems design, and
> agentic-AI integration relevant to aerospace manufacturing scheduling roles.

## Read these first

- [`docs/architecture.md`](docs/architecture.md) — system overview, component diagram, request flows
- [`docs/data-model.md`](docs/data-model.md) — Postgres schema (with table ownership) + DynamoDB + S3
- [`docs/api-specification.md`](docs/api-specification.md) — every HTTP endpoint and event shape
- [`docs/architecture-decisions.md`](docs/architecture-decisions.md) — why each major choice was made
- [`docs/infrastructure-spec.md`](docs/infrastructure-spec.md) — exact AWS resources, networking, env vars

## Status

- ✅ **Frontend** (`frontend/`) — all 4 screens built against mock data, tested in-browser,
  **deployed and live** (see Live demo below).
- ✅ **work-order-service** — rockets, work orders, operations, dependencies (cycle-rejected),
  resources, materials, audit log. 5/5 tests passing.
- ✅ **scheduling-service** — deterministic scheduling engine, schedule proposals
  (approve/reject with optimistic locking), launch-readiness rollup. 5/5 tests passing.
- ⏳ **event-processing-service**, **notification-service**, **agent-service** — not yet built.
- ⏳ **infrastructure/terraform** — only the frontend hosting piece is provisioned so far (see
  below); ECS/RDS/DynamoDB/Step Functions/SQS/SNS/API Gateway are not yet provisioned.

## Live demo

**Frontend:** http://aeroplan-frontend-9a86245a.s3-website.us-east-2.amazonaws.com

Deployed as a public S3 static website (not yet CloudFront — this AWS account needs AWS Support
to verify it before CloudFront distributions can be created; see
`infrastructure/terraform/README.md`). It's running against bundled mock data (`USING_MOCK_DATA`
in `frontend/src/api.ts`); no backend is wired up to it yet. HTTP only, no custom domain.

## Cost warning

This deploys **real AWS resources that cost money** if left running (RDS, ALB, 5× Fargate tasks
— see the cost estimate in `docs/infrastructure-spec.md`, ~$75–100/month if left running
continuously). Scale ECS services to 0 desired count and stop the RDS instance (or
`terraform destroy`) between demo sessions.

## Repository map

```
aeroplan-ai/
├── frontend/                        React + TypeScript dashboard
├── services/
│   ├── work-order-service/          Java 21 + Spring Boot — rockets, work orders, dependencies, BOM/materials
│   ├── scheduling-service/          Java 21 + Spring Boot — scheduling engine, launch-readiness planning, proposals
│   ├── event-processing-service/    Java 21 + Spring Boot — SQS consumer, downtime/test events, Step Functions trigger
│   ├── notification-service/        Java 21 + Spring Boot — SNS, email/Slack delivery
│   └── agent-service/               Python + FastAPI + LangGraph + Bedrock (6 agents)
├── algorithms/scheduling-optimizer/  Scheduling algorithm notes / future OR-Tools module
├── infrastructure/terraform/        All AWS infrastructure as code
├── workflows/step-functions/        Step Functions ASL definitions
├── tests/{integration,contract,load}
└── docs/                            Architecture, data model, API spec, ADRs, infra spec
```

## Run locally

Each service has its own `README.md` with instructions to run it standalone. A root
`docker-compose.yml` wiring Postgres + DynamoDB Local + all services together is added once
each service has a working Dockerfile.

## Deploy to AWS

See `docs/infrastructure-spec.md` and `infrastructure/terraform/README.md`.
