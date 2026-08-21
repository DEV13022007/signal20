import { useClock, formatUtc } from '../hooks/useClock'
import './StatusBar.css'

const PRIORITY_ORDER = ['MEDICAL', 'EQUIPMENT', 'SUPPLY', 'ROUTINE']
const PRIORITY_COLOR = {
  MEDICAL: 'var(--danger)',
  EQUIPMENT: 'var(--warn)',
  SUPPLY: 'var(--accent)',
  ROUTINE: 'var(--text-faint)',
}

export function StatusBar({ isOnline, servingFromCache, linkedCount, totalStations, syncStatus, criticalCount }) {
  const now = useClock()
  const pendingByPriority = syncStatus?.pendingByPriority ?? {}
  const totalPending = syncStatus?.totalPending ?? 0

  return (
    <header className="status-bar">
      <div className="status-bar__brand">
        <span className={`status-dot ${isOnline ? 'status-dot--online' : 'status-dot--offline'}`} />
        <span className="status-bar__title">POLARCONNECT</span>
        <span className="status-bar__mode">{isOnline ? 'LINK ACTIVE' : 'LOCAL CACHE'}</span>
      </div>

      <div className="status-bar__stats">
        <Stat label="Stations linked" value={`${linkedCount}/${totalStations}`} />
        <Stat label="Pending ops" value={totalPending} />
        <Stat label="Critical alerts" value={criticalCount} tone={criticalCount > 0 ? 'danger' : undefined} />
        <Stat label="Synced" value={syncStatus?.totalSynced ?? 0} />
      </div>

      <div className="status-bar__priority" title="Pending sync ops by priority">
        {PRIORITY_ORDER.map((p) => {
          const count = pendingByPriority[p] ?? 0
          const width = totalPending > 0 ? Math.max((count / totalPending) * 100, count > 0 ? 4 : 0) : 0
          return (
            <div
              key={p}
              className="status-bar__priority-seg"
              style={{ width: `${width}%`, background: PRIORITY_COLOR[p] }}
            />
          )
        })}
      </div>

      <div className="status-bar__clock mono">
        {servingFromCache && <span className="status-bar__cache-flag">CACHED</span>}
        {formatUtc(now)}
      </div>
    </header>
  )
}

function Stat({ label, value, tone }) {
  return (
    <div className="stat">
      <div className="stat__value mono" data-tone={tone}>
        {value}
      </div>
      <div className="stat__label eyebrow">{label}</div>
    </div>
  )
}
