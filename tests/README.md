# tests

- `integration/` — end-to-end scenarios spanning multiple services (e.g. publish a
  `MACHINE_DOWNTIME` event via SQS → assert a `ScheduleProposal` appears → approve it → assert a
  new published `ScheduleVersion` and the SNS notification). Owned by the integration pass once
  the individual services are each independently tested; see the five failure scenarios in the
  original project brief for what these should cover (duplicate events, consumer crash recovery,
  concurrent approval race, mid-optimization material change, invalid agent output).
- `contract/` — consumer-driven contract tests verifying the frontend's assumptions about
  `scheduling-service`'s API match reality, and `scheduling-service`'s assumptions about
  `agent-service`'s response shape match reality. Each service's own test suite should already
  cover its side; these tests close the loop across service boundaries.
- `load/` — basic throughput/latency benchmarks (API p95 latency, event-processing throughput,
  optimization time, recovery-workflow duration) per the "what makes this impressive" section of
  the original project brief. P2/stretch.

Nothing here yet — individual service workstreams should focus on their own unit/integration
tests first (see each service's `README.md`); this folder is for cross-service tests added
during the final integration pass.
