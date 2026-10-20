import { useEffect, useState } from 'react'
import { fetchLaunchReadiness, fetchRockets, fetchWorkOrders } from '../api'
import type { LaunchPreparation, Rocket, WorkOrder } from '../types'

export default function LaunchReadinessScreen() {
  const [rockets, setRockets] = useState<Rocket[] | null>(null)
  const [readiness, setReadiness] = useState<LaunchPreparation[] | null>(null)
  const [workOrders, setWorkOrders] = useState<WorkOrder[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [r, lr, wo] = await Promise.all([fetchRockets(), fetchLaunchReadiness(), fetchWorkOrders()])
        if (!cancelled) {
          setRockets(r)
          setReadiness(lr)
          setWorkOrders(wo)
        }
      } catch (reason) {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Could not load launch readiness')
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  if (error) return <div className="error-state" role="alert">{error}</div>
  if (!rockets || !readiness || !workOrders) return <div className="loading-state">Loading launch readiness...</div>

  return (
    <section className="panel">
      <p className="eyebrow">Rocket &amp; Launch Readiness</p>
      <h2>Vehicle Readiness Overview</h2>
      <div className="rocket-cards">
        {rockets.map((rocket) => {
          const prep = readiness.find((r) => r.rocketId === rocket.id)
          const woForRocket = workOrders.filter((w) => w.rocketId === rocket.id)
          const completed = woForRocket.filter((w) => w.status === 'COMPLETE').length
          const completionPct = woForRocket.length > 0 ? Math.round((completed / woForRocket.length) * 100) : 0
          const barClass =
            prep?.readinessStatus === 'BLOCKED' ? 'blocked' : prep?.readinessStatus === 'AT_RISK' ? 'at-risk' : ''

          return (
            <div className="rocket-card" key={rocket.id}>
              <div className="rocket-card-header">
                <div>
                  <h3>{rocket.name} {rocket.variant}</h3>
                  <span>{rocket.configuration}</span>
                </div>
                {prep && (
                  <span className={`badge readiness-${prep.readinessStatus.toLowerCase()}`}>
                    {prep.readinessStatus.replace(/_/g, ' ')}
                  </span>
                )}
              </div>

              <div className="readiness-score">
                <strong>{prep ? prep.readinessScore.toFixed(0) : '\u2014'}</strong>
                <span>/ 100 readiness score</span>
              </div>
              <div className="readiness-bar-track">
                <div className={`readiness-bar ${barClass}`} style={{ width: `${prep?.readinessScore ?? 0}%` }} />
              </div>

              <p className="rocket-card-notes">{prep?.notes ?? 'No readiness assessment yet.'}</p>

              <div className="metric-cards" style={{ gridTemplateColumns: 'repeat(3, 1fr)', marginBottom: 14 }}>
                <div className="metric-card">
                  <span>Work orders</span>
                  <strong>{completed}/{woForRocket.length}</strong>
                </div>
                <div className="metric-card">
                  <span>Completion</span>
                  <strong>{completionPct}%</strong>
                </div>
                <div className="metric-card">
                  <span>Weather</span>
                  <strong style={{ fontSize: 18 }}>
                    {prep?.weatherStatus === 'GO' ? '\u2705 GO' : prep?.weatherStatus === 'NO_GO' ? '\u26d4 NO-GO' : 'Unknown'}
                  </strong>
                </div>
              </div>

              <div className="rocket-card-footer">
                <span>Stage: {rocket.status.replace(/_/g, ' ')}</span>
                <span>
                  {prep?.plannedLaunchAt
                    ? `Launch window: ${new Date(prep.plannedLaunchAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric' })}`
                    : 'No launch window set'}
                </span>
              </div>

              {prep?.readinessStatus === 'READY_FOR_LAUNCH' && (
                <div className="ready-banner">{'\u2714 Ready for launch'}</div>
              )}
            </div>
          )
        })}
      </div>
    </section>
  )
}
