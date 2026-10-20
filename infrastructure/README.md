# infrastructure (Terraform)

All AWS infrastructure for AeroPlan AI as code. **This is the only workstream that runs
`terraform apply` against the real AWS account** (`420374519668`, region `us-east-2`, profile
`blue-origin-demo` is already authenticated via `aws login` in this environment — use
`-profile blue-origin-demo` or `AWS_PROFILE=blue-origin-demo` for every AWS CLI/Terraform
command). Other workstreams write Dockerfiles and read env vars; they don't provision cloud
resources themselves.

**This file is your scope.** Do not edit `../services/*` or `../frontend` source code — if a
service needs a Dockerfile and doesn't have one yet, note it and provide a minimal placeholder
so Terraform/CI isn't blocked, but let the owning workstream finalize the application code.

## Source of truth

`../docs/infrastructure-spec.md` is the frozen contract: exact resource names, networking
design (reuse default VPC, no NAT Gateway, internal ALB + VPC Link, Lambda HTTP bridge for Step
Functions), IAM scoping, and every environment variable name the other three workstreams expect
to be injected. Re-read it before writing any `.tf` file — do not redesign the network topology;
flag concerns instead of silently changing the approach, since the other three workstreams'
`.env.example` files and application config already assume these exact names.

`../workflows/step-functions/disruption-recovery.asl.json` is the Step Functions definition to
render via `templatefile()` with the `http_bridge_lambda_arn` variable.

## Suggested module layout

```
infrastructure/terraform/
├── main.tf              # provider, data sources (default VPC/subnets)
├── networking.tf        # ALB, target groups, listener rules, security groups, VPC Link
├── compute.tf            # ECS cluster, task definitions, services, ECR repos
├── data.tf               # RDS, DynamoDB tables, Secrets Manager
├── messaging.tf          # SQS + DLQ, SNS topic
├── orchestration.tf      # Step Functions state machine, sfn-http-bridge Lambda + IAM
├── edge.tf               # API Gateway HTTP API, S3 + CloudFront for frontend
├── iam.tf                # Per-service task roles/policies, GitHub OIDC role (P1)
├── variables.tf
├── outputs.tf            # API Gateway invoke URL, CloudFront domain, ALB DNS (for the Lambda)
└── README.md             # this file
```

## P0 checklist

1. `main.tf` with the `aws` provider pinned to `us-east-2`, plus `data` lookups for the default
   VPC and its subnets (do **not** create a new VPC — see `infrastructure-spec.md`).
2. Networking: internal ALB, 3 target groups (one per ECS service), path-based listener rules
   (`/api/*` + `/internal/*` → scheduling-service, `/events/*` → manufacturing-event-service,
   `/agents/*` → agent-service), security groups exactly as scoped in `infrastructure-spec.md`.
3. Data: RDS `aeroplan-db` (`db.t4g.micro`, single-AZ, `publicly_accessible=false`, credentials
   in Secrets Manager), DynamoDB `aeroplan-idempotency-records` + `aeroplan-workflow-state`
   (on-demand billing, TTL where specified).
4. Messaging: SQS `aeroplan-downtime-events` + DLQ with `maxReceiveCount=5` redrive, SNS
   `aeroplan-schedule-notifications` (optional email subscription via `notification_email`
   variable).
5. Orchestration: the `aeroplan-sfn-http-bridge` Lambda (VPC-attached, minimal IAM), the
   `aeroplan-disruption-recovery` Standard Step Functions state machine rendered from the ASL
   template file.
6. Compute: ECR repos for all 3 services, ECS cluster + Fargate task definitions/services sized
   per `infrastructure-spec.md`, reading config from Secrets Manager/env vars exactly as named
   in that doc's environment-variable tables. (If a service's Dockerfile isn't ready yet, use a
   placeholder `nginx:alpine` or similar so the ECS service/task-definition resources can still
   be authored and `terraform plan` succeeds — swap the image tag once the real one exists.)
7. Edge: API Gateway HTTP API + VPC Link to the internal ALB; S3 bucket (private, OAC) +
   CloudFront distribution for the frontend build output.
8. IAM: least-privilege task roles exactly as scoped in `infrastructure-spec.md` (including
   `states:SendTaskSuccess`/`SendTaskFailure` for `scheduling-service`).
9. `outputs.tf` exposing the API Gateway invoke URL, CloudFront domain, and ALB internal DNS
   name — other workstreams and the root README need these.
10. A `terraform.tfvars.example` documenting every required variable (region is fixed at
    `us-east-2`; don't make it a variable that could accidentally be overridden to an
    unintended region).

## Definition of done for P0

`terraform plan` succeeds with no errors against account `420374519668` (profile
`blue-origin-demo`). `terraform apply` provisions the full stack (placeholder images are fine
if application code isn't finished). Document the exact `terraform apply`/`destroy` commands in
this README, and call out clearly that `terraform destroy` (or scaling resources down) should be
run between sessions to control cost (see the cost estimate in `infrastructure-spec.md`).
