import { useEffect, useMemo, useState } from 'react'
import { fetchProposals, fetchResources, fetchWorkOrders } from '../api'
import type { Resource, ScheduleProposal, WorkOrder } from '../types'

export default function FactoryHealth() {
  const [workOrders, setWorkOrders] = useState<WorkOrder[] | null>(null)
  const [resources, setResources] = useState<Resource[] | null>(null)
  const [proposals, setProposals] = useState<ScheduleProposal[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [wo, res, prop] = await Promise.all([fetchWorkOrders(), fetchResources(), fetchProposals()])
        if (!cancelled) {
          setWorkOrders(wo)
          setResources(res)
          setProposals(prop)
        }
      } catch (reason) {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Could not load factory health')
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  const stats = useMemo(() => {
    if (!workOrders) return null
    const now = Date.now()
    const overdue = workOrders.filter((w) => new Date(w.dueAt).getTime() < now && w.status !== 'COMPLETE').length
    const atRisk = workOrders.filter((w) => w.status === 'BLOCKED' || w.status === 'LATE').length
    return { total: workOrders.length, overdue, atRisk }
  }, [workOrders])

  const downResources = useMemo(() => (resources ?? []).filter((r) => r.availabilityStatus !== 'AVAILABLE'), [resources])
  const utilizationPct = useMemo(() => {
    if (!resources || resources.length === 0) return 0
    const busy = resources.filter((r) => r.availabilityStatus === 'AVAILABLE').length
    return Math.round((busy / resources.length) * 85) // demo heuristic, not a literal occupancy calc
  }, [resources])

  const bottlenecks = useMemo(() => {
    const latest = (proposals ?? []).find((p) => p.status === 'PENDING')
    return latest?.agentFindings.bottleneckRisk.atRiskWorkOrders ?? []
  }, [proposals])

  if (error) return <div className="error-state" role="alert">{error}</div>
  if (!workOrders || !resources || !stats) return <div className="loading-state">Loading factory health...</div>

  return (
    <section className="panel">
      <p className="eyebrow">Factory Health Dashboard</p>
      <h2>Operational Overview</h2>

      <div className="metric-cards">
        <div className="metric-card info">
          <span>Total work orders</span>
          <strong>{stats.total}</strong>
        </div>
        <div className="metric-card warn">
          <span>Overdue</span>
          <strong>{stats.overdue}</strong>
        </div>
        <div className="metric-card danger">
          <span>At risk / blocked</span>
          <strong>{stats.atRisk}</strong>
        </div>
        <div className="metric-card danger">
          <span>Resources down</span>
          <strong>{downResources.length}</strong>
        </div>
      </div>

      <div className="health-columns">
        <div className="health-card">
          <h3>Resource status</h3>
          <ul className="resource-list">
            {resources.map((r) => (
              <li key={r.id}>
                <span>{r.name}</span>
                <span className={`badge resource-${r.availabilityStatus.toLowerCase()}`}>{r.availabilityStatus}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="health-card">
          <h3>Resource utilization</h3>
          <div className="utilization-ring-wrap">
            <div className="utilization-value">{utilizationPct}%</div>
            <div className="readiness-bar-track" style={{ flex: 1 }}>
              <div className="readiness-bar" style={{ width: `${utilizationPct}%` }} />
            </div>
          </div>

          <h3 style={{ marginTop: 20 }}>Top bottlenecks</h3>
          {bottlenecks.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: 12, margin: 0 }}>No active bottlenecks detected.</p>
          ) : (
            <ul className="bottleneck-list">
              {bottlenecks.map((b) => {
                const wo = workOrders.find((w) => w.id === b.workOrderId)
                return (
                  <li key={b.workOrderId}>
                    <span>{wo ? `${wo.partNumber} \u2014 ${wo.title}` : `Work order ${b.workOrderId}`}</span>
                    <span>{Math.round(b.lateProbability * 100)}% late risk</span>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
