import { useMemo } from 'react'
import type { Operation, Priority, Resource, ScheduleAssignment, WorkOrder } from '../types'

// Hand-rolled Gantt chart (SVG/CSS only, no charting dependency — see ADR-006 in
// docs/architecture-decisions.md). One row per resource; each assignment is rendered as a bar
// positioned by percentage within the visible time window, colored by the owning work order's
// priority so a planner can spot critical-path risk at a glance.

type Props = {
  resources: Resource[]
  assignments: ScheduleAssignment[]
  operations: Operation[]
  workOrders: WorkOrder[]
  windowStart: Date
  windowEnd: Date
}

function priorityOf(operationId: number, operations: Operation[], workOrders: WorkOrder[]): Priority {
  const op = operations.find((o) => o.id === operationId)
  const wo = workOrders.find((w) => w.id === op?.workOrderId)
  return wo?.priority ?? 'LOW'
}

function labelOf(operationId: number, operations: Operation[], workOrders: WorkOrder[]): string {
  const op = operations.find((o) => o.id === operationId)
  const wo = workOrders.find((w) => w.id === op?.workOrderId)
  return wo ? `${wo.partNumber} \u2014 ${op?.name ?? ''}` : (op?.name ?? `Operation ${operationId}`)
}

export default function GanttChart({ resources, assignments, operations, workOrders, windowStart, windowEnd }: Props) {
  const totalMs = windowEnd.getTime() - windowStart.getTime()
  const pct = (iso: string) => {
    const t = new Date(iso).getTime()
    return Math.min(100, Math.max(0, ((t - windowStart.getTime()) / totalMs) * 100))
  }
  const now = useMemo(() => new Date(), [])
  const nowPct = pct(now.toISOString())
  const showNowLine = now >= windowStart && now <= windowEnd

  const hourMarks = []
  const hours = Math.round(totalMs / 3600_000)
  const step = hours > 36 ? 12 : hours > 18 ? 6 : 3
  for (let h = 0; h <= hours; h += step) {
    hourMarks.push(h)
  }

  return (
    <div className="gantt" aria-label="Schedule Gantt chart">
      <div className="gantt-header">
        {hourMarks.map((h) => (
          <span key={h} style={{ flex: 1, textAlign: 'left' }}>
            {new Date(windowStart.getTime() + h * 3600_000).toLocaleString(undefined, {
              weekday: 'short',
              hour: 'numeric',
            })}
          </span>
        ))}
      </div>
      {resources.map((resource) => {
        const rows = assignments.filter((a) => a.resourceId === resource.id)
        return (
          <div className="gantt-row" key={resource.id}>
            <div className="gantt-row-label">
              {resource.name}
              <span>{resource.resourceType.replace('_', ' ')}</span>
            </div>
            <div className="gantt-track">
              {showNowLine && <div className="gantt-now-line" style={{ left: `${nowPct}%` }} title="Now" />}
              {rows.map((a) => {
                const left = pct(a.startAt)
                const width = Math.max(1.5, pct(a.endAt) - left)
                const priority = priorityOf(a.operationId, operations, workOrders)
                return (
                  <div
                    key={a.operationId}
                    className={`gantt-bar priority-${priority.toLowerCase()}`}
                    style={{ left: `${left}%`, width: `${width}%` }}
                    title={`${labelOf(a.operationId, operations, workOrders)}\n${new Date(a.startAt).toLocaleString()} \u2192 ${new Date(a.endAt).toLocaleString()}`}
                  >
                    {labelOf(a.operationId, operations, workOrders)}
                  </div>
                )
              })}
            </div>
          </div>
        )
      })}
      {resources.length === 0 && (
        <div className="empty-state">
          <strong>No resources to display</strong>
        </div>
      )}
    </div>
  )
}
