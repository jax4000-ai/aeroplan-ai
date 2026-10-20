import { useEffect, useMemo, useState } from 'react'
import { fetchCurrentSchedule, fetchResources, fetchWorkOrders } from '../api'
import type { Priority, Resource, ScheduleVersion, WorkOrder, WorkOrderStatus } from '../types'
import GanttChart from '../components/GanttChart'

type Props = { rocketId: number | null }

export default function ManufacturingSchedule({ rocketId }: Props) {
  const [workOrders, setWorkOrders] = useState<WorkOrder[] | null>(null)
  const [resources, setResources] = useState<Resource[] | null>(null)
  const [schedule, setSchedule] = useState<ScheduleVersion | null>(null)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<WorkOrderStatus | 'ALL'>('ALL')
  const [stageFilter, setStageFilter] = useState<'ALL' | WorkOrder['workType']>('ALL')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [wo, res, sched] = await Promise.all([
          fetchWorkOrders(rocketId != null ? { rocketId } : undefined),
          fetchResources(),
          fetchCurrentSchedule(),
        ])
        if (!cancelled) {
          setWorkOrders(wo)
          setResources(res)
          setSchedule(sched)
        }
      } catch (reason) {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Could not load schedule')
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [rocketId])

  const filtered = useMemo(() => {
    if (!workOrders) return []
    return workOrders.filter((wo) => {
      if (statusFilter !== 'ALL' && wo.status !== statusFilter) return false
      if (stageFilter !== 'ALL' && wo.workType !== stageFilter) return false
      if (query && !(`${wo.partNumber} ${wo.title}`.toLowerCase().includes(query.toLowerCase()))) return false
      return true
    })
  }, [workOrders, statusFilter, stageFilter, query])

  const operations = useMemo(() => (workOrders ?? []).flatMap((wo) => wo.operations), [workOrders])
  const windowBounds = useMemo((): [Date, Date] => {
    const nowMs = Date.now()
    return [new Date(nowMs - 6 * 3600_000), new Date(nowMs + 36 * 3600_000)]
  }, [])

  if (error) return <div className="error-state" role="alert">{error}</div>
  if (!workOrders || !resources || !schedule) return <div className="loading-state">Loading manufacturing schedule...</div>

  const [windowStart, windowEnd] = windowBounds

  return (
    <>
      <section className="panel">
        <p className="eyebrow">Production Schedule</p>
        <h2>Manufacturing &amp; Assembly Schedule</h2>
        <GanttChart
          resources={resources}
          assignments={schedule.assignments}
          operations={operations}
          workOrders={workOrders}
          windowStart={windowStart}
          windowEnd={windowEnd}
        />
      </section>

      <section className="panel">
        <p className="eyebrow">Work Orders</p>
        <h2>Active &amp; Planned Work</h2>
        <div className="toolbar">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search work orders..."
            aria-label="Search work orders"
          />
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as WorkOrderStatus | 'ALL')} aria-label="Filter by status">
            <option value="ALL">All statuses</option>
            <option value="PLANNED">Planned</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="COMPLETE">Complete</option>
            <option value="LATE">Late</option>
            <option value="BLOCKED">Blocked</option>
          </select>
          <select value={stageFilter} onChange={(e) => setStageFilter(e.target.value as 'ALL' | WorkOrder['workType'])} aria-label="Filter by stage">
            <option value="ALL">All stages</option>
            <option value="MANUFACTURING">Manufacturing</option>
            <option value="ASSEMBLY">Assembly</option>
            <option value="INTEGRATION">Integration</option>
            <option value="TEST">Test</option>
            <option value="LAUNCH_PREP">Launch prep</option>
          </select>
        </div>

        {filtered.length === 0 ? (
          <div className="empty-state">
            <strong>No work orders match your filters</strong>
            <p>Try clearing the search or changing a filter.</p>
          </div>
        ) : (
          <table className="work-order-table">
            <thead>
              <tr>
                <th>Part #</th>
                <th>Title</th>
                <th>Stage</th>
                <th>Priority</th>
                <th>Due</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((wo) => (
                <tr key={wo.id}>
                  <td>{wo.partNumber}</td>
                  <td>{wo.title}</td>
                  <td>{wo.workType.replace('_', ' ')}</td>
                  <td><span className={`badge priority-${(wo.priority as Priority).toLowerCase()}`}>{wo.priority}</span></td>
                  <td>{new Date(wo.dueAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric' })}</td>
                  <td><span className={`badge status-${wo.status.toLowerCase()}`}>{wo.status.replace('_', ' ')}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
