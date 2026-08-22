import { useClock, formatUtc } from '../hooks/useClock'
import { PRIORITIES } from '../constants/priority'
import './StatusBar.css'

const PRIORITY_COLOR = {
  MEDICAL: 'var(--danger)',
  EQUIPMENT: 'var(--warn)',
  SUPPLY: 'var(--accent)',
  ROUTINE: 'var(--text-faint)',
}

const LINK_STATE = {
  none: { dot: 'status-dot--none', label: 'NO STATIONS' },
  down: { dot: 'status-dot--down', label: 'ALL LINKS DOWN' },
  partial: { dot: 'status-dot--partial', label: 'PARTIAL LINK' },
  all: { dot: 'status-dot--all', label: 'ALL LINKED' },
}

function linkState(linkedCount, totalStations) {
  if (totalStations === 0) return 'none'
  if (linkedCount === 0) return 'down'
  if (linkedCount === totalStations) return 'all'
  return 'partial'
}

export function StatusBar({
  servingFromCache,
  linkedCount,
  totalStations,
  syncStatus,
  criticalCount,
  simulatedOffline,
  onToggleSimulatedOffline,
}) {
  const now = useClock()
  const pendingByPriority = syncStatus?.pendingByPriority ?? {}
  const totalPending = syncStatus?.totalPending ?? 0
  const { dot, label } = LINK_STATE[linkState(linkedCount, totalStations)]

  return (
    <header className="status-bar">
      <div className="status-bar__brand">
        <span className={`status-dot ${dot}`} />
        <span className="status-bar__title">POLARCONNECT</span>
        <span className="status-bar__mode">{label}</span>
        <span className={`browser-link ${simulatedOffline ? 'browser-link--down' : 'browser-link--up'}`}>
          {simulatedOffline ? 'BROWSER OFFLINE' : 'BROWSER ONLINE'}
        </span>
        <button className="sim-offline-btn" onClick={onToggleSimulatedOffline} type="button">
          {simulatedOffline ? 'Restore connection' : 'Simulate offline'}
        </button>
      </div>

      <div className="status-bar__stats">
        <Stat label="Stations linked" value={`${linkedCount}/${totalStations}`} />
        <Stat label="Pending ops" value={totalPending} />
        <Stat label="Critical alerts" value={criticalCount} tone={criticalCount > 0 ? 'danger' : undefined} />
        <Stat label="Synced" value={syncStatus?.totalSynced ?? 0} />
      </div>

      <div className="status-bar__priority" title="Pending sync ops by priority">
        {PRIORITIES.map((p) => {
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
