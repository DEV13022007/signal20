import './SyncQueuePanel.css'

const STATUS_ORDER = { FAILED: 0, PENDING: 1, SYNCED: 2 }

function timeAgo(iso) {
  if (!iso) return '—'
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000))
  if (seconds < 60) return `${seconds}s ago`
  if (seconds < 3600) return `${Math.round(seconds / 60)}m ago`
  return `${Math.round(seconds / 3600)}h ago`
}

export function SyncQueuePanel({ records, stationsById }) {
  const sorted = [...records]
    .sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || b.createdAt.localeCompare(a.createdAt))
    .slice(0, 40)

  return (
    <aside className="sync-panel" aria-label="Sync queue">
      <div className="eyebrow sync-panel__heading">Sync queue · {records.length}</div>
      <ul className="sync-panel__list">
        {sorted.map((record) => (
          <li key={record.id} className={`sync-entry sync-entry--${record.status.toLowerCase()}`}>
            <div className="sync-entry__top">
              <span className="sync-entry__station">{stationsById[record.stationId]?.code ?? '—'}</span>
              <span className={`sync-entry__status status-chip status-chip--${statusTone(record.status)}`}>
                {record.status}
              </span>
            </div>
            <div className="sync-entry__body">
              <span className="mono">{record.entityType} #{record.entityId}</span>
              <span className="sync-entry__op">{record.operation}</span>
            </div>
            <div className="sync-entry__meta">
              <span>{timeAgo(record.syncedAt ?? record.createdAt)}</span>
              {record.retryCount > 0 && <span className="sync-entry__retry">retry ×{record.retryCount}</span>}
            </div>
          </li>
        ))}
        {sorted.length === 0 && <li className="sync-panel__empty">Queue is empty.</li>}
      </ul>
    </aside>
  )
}

function statusTone(status) {
  if (status === 'FAILED') return 'danger'
  if (status === 'PENDING') return 'warn'
  return 'ok'
}
