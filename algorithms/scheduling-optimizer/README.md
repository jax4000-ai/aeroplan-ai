# algorithms/scheduling-optimizer

This folder exists for a **future** upgrade path: replacing/augmenting the greedy
list-scheduling algorithm inside `services/scheduling-service` with a proper
resource-constrained project scheduling (RCPSP) solver, e.g. Google OR-Tools' CP-SAT.

For P0, the scheduling algorithm lives directly in `services/scheduling-service` (Java) since
it needs tight transactional coupling with the domain entities. If/when an OR-Tools-based
optimizer is added (P2/stretch), it would likely run as its own small service (Python, since
OR-Tools' Python bindings are the most mature) called by `scheduling-service` for the
`/api/schedules/optimize` endpoint, with the existing greedy algorithm kept as a fast fallback.

Nothing here yet — this is a placeholder documenting the intended extension point.
