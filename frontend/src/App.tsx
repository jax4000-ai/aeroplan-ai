import { useState } from 'react'
import { USING_MOCK_DATA } from './api'
import ManufacturingSchedule from './screens/ManufacturingSchedule'
import LaunchReadinessScreen from './screens/LaunchReadinessScreen'
import FactoryHealth from './screens/FactoryHealth'
import AIRecoveryProposals from './screens/AIRecoveryProposals'
import './App.css'

type View = 'schedule' | 'readiness' | 'health' | 'proposals'

const TABS: { id: View; label: string }[] = [
  { id: 'schedule', label: 'Manufacturing & Assembly Schedule' },
  { id: 'readiness', label: 'Rocket & Launch Readiness' },
  { id: 'health', label: 'Factory Health' },
  { id: 'proposals', label: 'AI Recovery Proposals' },
]

function App() {
  const [view, setView] = useState<View>('schedule')

  return (
    <main className="app-shell">
      <div className="demo-notice" role="note">
        <strong>INTERVIEW DEMO</strong>
        Synthetic aerospace manufacturing data only. Not affiliated with Blue Origin or any employer.
        {USING_MOCK_DATA && ' Backend services not yet connected — showing mock fixture data.'}
      </div>

      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">A</span>
          <div>
            <strong>AeroPlan AI</strong>
            <span>Rocket manufacturing, assembly &amp; launch scheduling platform</span>
          </div>
        </div>
        <nav className="view-tabs" role="tablist" aria-label="View">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={view === tab.id}
              className={view === tab.id ? 'active' : ''}
              onClick={() => setView(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </header>

      {view === 'schedule' && <ManufacturingSchedule rocketId={null} />}
      {view === 'readiness' && <LaunchReadinessScreen />}
      {view === 'health' && <FactoryHealth />}
      {view === 'proposals' && <AIRecoveryProposals />}
    </main>
  )
}

export default App
