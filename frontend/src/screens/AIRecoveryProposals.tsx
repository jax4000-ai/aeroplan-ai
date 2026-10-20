import { useEffect, useState } from 'react'
import { decideProposal, fetchProposals, fetchWorkOrders } from '../api'
import type { ScheduleProposal, WorkOrder } from '../types'

export default function AIRecoveryProposals() {
  const [proposals, setProposals] = useState<ScheduleProposal[] | null>(null)
  const [workOrders, setWorkOrders] = useState<WorkOrder[] | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [deciding, setDeciding] = useState(false)

  async function load() {
    try {
      const [p, wo] = await Promise.all([fetchProposals(), fetchWorkOrders()])
      setProposals(p)
      setWorkOrders(wo)
      setSelectedId((current) => current ?? p[0]?.id ?? null)
      setError('')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not load proposals')
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleDecision(id: number, decision: 'APPROVE' | 'REJECT') {
    setDeciding(true)
    setError('')
    try {
      await decideProposal(id, decision, 'planner.ada')
      await load()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not record decision')
    } finally {
      setDeciding(false)
    }
  }

  if (error && !proposals) return <div className="error-state" role="alert">{error}</div>
  if (!proposals || !workOrders) return <div className="loading-state">Loading recovery proposals...</div>

  const selected = proposals.find((p) => p.id === selectedId) ?? null

  return (
    <section className="panel">
      <p className="eyebrow">AI Recovery Proposals</p>
      <h2>Disruption Recovery</h2>

      {proposals.length === 0 ? (
        <div className="empty-state">
          <strong>No active proposals</strong>
          <p>Everything is running on the published schedule.</p>
        </div>
      ) : (
        <div className="proposal-list">
          {proposals.map((p) => (
            <article
              key={p.id}
              className={`proposal-card ${p.id === selectedId ? 'selected' : ''}`}
              onClick={() => setSelectedId(p.id)}
            >
              <header>
                <strong>Proposal #{p.id} {'\u2014'} {p.machineDowntime.reason}</strong>
                <span className={`badge status-${p.status === 'PENDING' ? 'in_progress' : p.status === 'APPROVED' ? 'complete' : 'late'}`}>
                  {p.status}
                </span>
              </header>
              <p>{p.agentSummary}</p>
            </article>
          ))}
        </div>
      )}

      {selected && (
        <div className="proposal-detail">
          <h2>Proposal #{selected.id} Detail</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{selected.agentSummary}</p>

          <div className="comparison-grid">
            <div className="comparison-card">
              <h4>Current published schedule</h4>
              <div className="comparison-stat"><span>Version</span><span>#{selected.basedOnScheduleVersionId}</span></div>
              <div className="comparison-stat"><span>Late work orders</span><span>{'\u2014'}</span></div>
            </div>
            {selected.agentFindings.schedulingOptimization.candidates.map((c, idx) => (
              <div
                key={c.scheduleVersionId}
                className={`comparison-card ${idx === selected.agentFindings.recovery.recommendedCandidateIndex ? 'recommended' : ''}`}
              >
                <h4>
                  Candidate {String.fromCharCode(65 + idx)}
                  {idx === selected.agentFindings.recovery.recommendedCandidateIndex ? ' \u2605 Recommended' : ''}
                </h4>
                <div className="comparison-stat"><span>Objective score</span><span>{c.objectiveScore?.toFixed(1) ?? '\u2014'}</span></div>
                <div className="comparison-stat"><span>Late work orders</span><span>{c.lateWorkOrders.length}</span></div>
                <div className="comparison-stat"><span>Overtime</span><span>{c.overtimeMinutesUsed} min</span></div>
                <div className="comparison-stat"><span>Changed assignments</span><span>{c.changedAssignments}</span></div>
              </div>
            ))}
          </div>

          <div className="findings-grid">
            <div className="finding-card">
              <h4>Materials readiness</h4>
              <p>
                {selected.agentFindings.materialsReadiness.blockedOperations.length === 0
                  ? 'No operations blocked by material shortages.'
                  : `${selected.agentFindings.materialsReadiness.blockedOperations.length} operation(s) blocked.`}
              </p>
            </div>
            <div className="finding-card">
              <h4>Bottleneck &amp; risk</h4>
              {selected.agentFindings.bottleneckRisk.atRiskWorkOrders.length === 0 ? (
                <p>No elevated lateness risk detected.</p>
              ) : (
                <ul>
                  {selected.agentFindings.bottleneckRisk.atRiskWorkOrders.map((r) => {
                    const wo = workOrders.find((w) => w.id === r.workOrderId)
                    return (
                      <li key={r.workOrderId}>
                        {wo?.partNumber ?? r.workOrderId}: {Math.round(r.lateProbability * 100)}% late risk
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
            <div className="finding-card">
              <h4>Launch readiness impact</h4>
              <p>
                {selected.agentFindings.launchReadiness.readinessImpact === 'NONE'
                  ? 'No impact to any launch window.'
                  : `${selected.agentFindings.launchReadiness.readinessImpact} \u2014 affects rocket(s) ${selected.agentFindings.launchReadiness.affectedRocketIds.join(', ')}`}
              </p>
            </div>
            <div className="finding-card">
              <h4>Recovery recommendation</h4>
              <p>{selected.agentFindings.recovery.tradeoffs}</p>
            </div>
          </div>

          {selected.status === 'PENDING' ? (
            <div className="proposal-actions">
              <button className="btn btn-approve" disabled={deciding} onClick={() => handleDecision(selected.id, 'APPROVE')}>
                {deciding ? 'Working...' : 'Approve'}
              </button>
              <button className="btn btn-reject" disabled={deciding} onClick={() => handleDecision(selected.id, 'REJECT')}>
                {deciding ? 'Working...' : 'Reject'}
              </button>
            </div>
          ) : (
            <div className={`decided-banner ${selected.status.toLowerCase()}`}>
              {selected.status} by {selected.decidedBy} at {selected.decidedAt ? new Date(selected.decidedAt).toLocaleString() : ''}
            </div>
          )}

          {error && <div className="error-state" role="alert" style={{ marginTop: 12 }}>{error}</div>}
        </div>
      )}
    </section>
  )
}
