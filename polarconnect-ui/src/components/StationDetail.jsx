import { useState } from 'react'
import { repo } from '../api/offlineRepo'
import './StationDetail.css'

const PRIORITY_ORDER = { MEDICAL: 0, EQUIPMENT: 1, SUPPLY: 2, ROUTINE: 3 }
const EXPIRY_WARNING_DAYS = 30

function daysUntil(dateStr) {
  if (!dateStr) return null
  const diff = new Date(dateStr).getTime() - new Date().setHours(0, 0, 0, 0)
  return Math.round(diff / 86_400_000)
}

function coord(lat, lon) {
  if (lat == null || lon == null) return '— , —'
  const ns = lat >= 0 ? 'N' : 'S'
  const ew = lon >= 0 ? 'E' : 'W'
  return `${Math.abs(lat).toFixed(2)}°${ns} ${Math.abs(lon).toFixed(2)}°${ew}`
}

export function StationDetail({ station, items, onChanged }) {
  const [busy, setBusy] = useState(false)

  if (!station) {
    return (
      <div className="station-detail station-detail--empty">
        <p>Select a station to view its inventory and link status.</p>
      </div>
    )
  }

  const sorted = [...items].sort((a, b) => PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority])

  async function toggleLink() {
    setBusy(true)
    try {
      await repo.setSatelliteLink(station.id, !station.satelliteLinkActive)
      await onChanged()
    } finally {
      setBusy(false)
    }
  }

  async function flush() {
    setBusy(true)
    try {
      await repo.flushStation(station.id)
      await onChanged()
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="station-detail">
      <div className="station-detail__header">
        <div>
          <div className="eyebrow">{station.code} · {station.country ?? 'Unknown territory'}</div>
          <h1 className="station-detail__name">{station.name}</h1>
          <div className="station-detail__coord mono">{coord(station.latitude, station.longitude)}</div>
        </div>
        <div className="station-detail__actions">
          <button
            className={`link-toggle ${station.satelliteLinkActive ? 'link-toggle--up' : 'link-toggle--down'}`}
            onClick={toggleLink}
            disabled={busy}
          >
            {station.satelliteLinkActive ? 'Link active — mark down' : 'Link down — mark active'}
          </button>
          <button className="flush-btn" onClick={flush} disabled={busy}>
            Flush sync queue
          </button>
        </div>
      </div>

      <div className="station-detail__section-title eyebrow">Inventory · {sorted.length} items</div>
      <table className="inventory-table">
        <thead>
          <tr>
            <th>Item</th>
            <th>Priority</th>
            <th>Qty</th>
            <th>Threshold</th>
            <th>Expiry</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((item) => {
            const low = item.minThreshold != null && item.quantity <= item.minThreshold
            const remaining = daysUntil(item.expiryDate)
            const expiring = remaining != null && remaining <= EXPIRY_WARNING_DAYS
            const expired = remaining != null && remaining < 0
            return (
              <tr key={item.id} className={low || expired ? 'inventory-row--alert' : ''}>
                <td>{item.name}</td>
                <td>
                  <span className={`priority-badge priority-badge--${item.priority.toLowerCase()}`}>
                    {item.priority}
                  </span>
                </td>
                <td className="mono">{item.quantity} {item.unit ?? ''}</td>
                <td className="mono">{item.minThreshold ?? '—'}</td>
                <td className="mono">{item.expiryDate ?? '—'}</td>
                <td>
                  {expired && <span className="status-chip status-chip--danger">Expired</span>}
                  {!expired && expiring && <span className="status-chip status-chip--warn">Expiring · {remaining}d</span>}
                  {!expired && low && <span className="status-chip status-chip--warn">Low stock</span>}
                  {!expired && !expiring && !low && <span className="status-chip status-chip--ok">Nominal</span>}
                </td>
              </tr>
            )
          })}
          {sorted.length === 0 && (
            <tr>
              <td colSpan={6} className="inventory-table__empty">No inventory recorded for this station.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
