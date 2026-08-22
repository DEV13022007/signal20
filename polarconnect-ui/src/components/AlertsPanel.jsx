import './AlertsPanel.css'

function timeAgo(iso) {
  if (!iso) return '—'
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000))
  if (seconds < 60) return `${seconds}s ago`
  if (seconds < 3600) return `${Math.round(seconds / 60)}m ago`
  return `${Math.round(seconds / 3600)}h ago`
}

export function AlertsPanel({ alerts, connected }) {
  const visible = alerts.slice(0, 40)

  return (
    <aside className="alerts-panel" aria-label="Live alerts">
      <div className="eyebrow alerts-panel__heading">
        Alerts · {alerts.length}
        <span className={`alerts-panel__link ${connected ? 'alerts-panel__link--up' : 'alerts-panel__link--down'}`}>
          {connected ? 'LIVE' : 'CONNECTING…'}
        </span>
      </div>
      <ul className="alerts-panel__list">
        {visible.map((alert) => (
          <li key={alert.id} className={`alert-entry alert-entry--${alert.severity.toLowerCase()}`}>
            <div className="alert-entry__top">
              <span className="alert-entry__station">{alert.stationName ?? '—'}</span>
              <span className={`status-chip status-chip--${alert.severity === 'CRITICAL' ? 'danger' : 'warn'}`}>
                {alert.severity}
              </span>
            </div>
            <div className="alert-entry__message">{alert.message}</div>
            <div className="alert-entry__meta">
              <span>{alert.category.replaceAll('_', ' ')}</span>
              <span>{timeAgo(alert.createdAt)}</span>
            </div>
          </li>
        ))}
        {visible.length === 0 && <li className="alerts-panel__empty">No alerts yet.</li>}
      </ul>
    </aside>
  )
}
