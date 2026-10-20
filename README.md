# AeroPlan AI

Aerospace manufacturing scheduling and AI-assisted disruption-recovery platform. A simulated
rocket-engine factory schedules work orders against limited stations/technicians/materials, and
a multi-agent AI workflow proposes recovery plans when something breaks — subject to mandatory
human approval before anything is published.

> **Independent portfolio/demo project.** Synthetic data only. Not affiliated with Blue Origin
> or any employer; built to demonstrate backend engineering, distributed-systems design, and
> agentic-AI integration relevant to aerospace manufacturing scheduling roles.

## Read these first

- [`docs/architecture.md`](docs/architecture.md) — system overview, component diagram, request flows
- [`docs/data-model.md`](docs/data-model.md) — Postgres schema + DynamoDB tables
- [`docs/api-specification.md`](docs/api-specification.md) — every HTTP endpoint and event shape
- [`docs/architecture-decisions.md`](docs/architecture-decisions.md) — why each major choice was made
- [`docs/infrastructure-spec.md`](docs/infrastructure-spec.md) — exact AWS resources, networking, env vars

## Status

See the checklist in `docs/architecture.md`'s priority tiers. This README's status section is
updated as each workstream lands.

## Cost warning

This deploys **real AWS resources that cost money** if left running (RDS, ALB, Fargate tasks —
see the cost estimate in `docs/infrastructure-spec.md`). Scale ECS services to 0 desired count
and stop the RDS instance (or `terraform destroy`) between demo sessions.

## Repository map

```
aeroplan-ai/
├── frontend/                        React + TypeScript dashboard
├── services/
│   ├── scheduling-service/          Java 21 + Spring Boot — work orders, scheduling engine, API
│   ├── manufacturing-event-service/ Java 21 + Spring Boot — SQS consumer, Step Functions trigger
│   └── agent-service/               Python + FastAPI + LangGraph + Bedrock
├── algorithms/scheduling-optimizer/  Scheduling algorithm notes / future OR-Tools module
├── infrastructure/terraform/        All AWS infrastructure as code
├── workflows/step-functions/        Step Functions ASL definitions
├── tests/{integration,contract,load}
└── docs/                            Architecture, data model, API spec, ADRs, infra spec
```

## Run locally

Each service has its own `README.md` with instructions to run it standalone. A root
`docker-compose.yml` wiring Postgres + DynamoDB Local + all three services together is added
once each service has a working Dockerfile.

## Deploy to AWS

See `docs/infrastructure-spec.md` and `infrastructure/terraform/README.md`.
