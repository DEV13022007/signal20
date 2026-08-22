import { useState } from 'react'
import { repo } from '../api/offlineRepo'
import './EquipmentPanel.css'

const STATUSES = ['OPERATIONAL', 'DEGRADED', 'FAILED']
const STATUS_ORDER = { FAILED: 0, DEGRADED: 1, OPERATIONAL: 2 }

function daysUntil(dateStr) {
  if (!dateStr) return null
  const diff = new Date(dateStr).getTime() - new Date().setHours(0, 0, 0, 0)
  return Math.round(diff / 86_400_000)
}

export function EquipmentPanel({ equipment, onChanged }) {
  const [busyId, setBusyId] = useState(null)

  const sorted = [...equipment].sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status])

  async function changeStatus(item, status) {
    if (status === item.status) return
    setBusyId(item.id)
    try {
      await repo.updateEquipment(item.id, { ...item, status })
      await onChanged()
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="equipment-panel">
      <div className="station-detail__section-title eyebrow">Equipment · {sorted.length}</div>
      <table className="equipment-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Last serviced</th>
            <th>Next service due</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((item) => {
            const overdueDays = daysUntil(item.nextServiceDue)
            const overdue = overdueDays != null && overdueDays < 0
            return (
              <tr key={item.id} className={item.status !== 'OPERATIONAL' || overdue ? 'equipment-row--alert' : ''}>
                <td>{item.name}</td>
                <td className="mono">{item.type}</td>
                <td className="mono">{item.lastServiceDate ?? '—'}</td>
                <td className="mono">
                  {item.nextServiceDue ?? '—'}
                  {overdue && (
                    <span className="status-chip status-chip--danger equipment-overdue">
                      Overdue · {Math.abs(overdueDays)}d
                    </span>
                  )}
                </td>
                <td>
                  <select
                    className={`equipment-select equipment-select--${item.status.toLowerCase()}`}
                    value={item.status}
                    disabled={busyId === item.id}
                    onChange={(e) => changeStatus(item, e.target.value)}
                  >
                    {STATUSES.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            )
          })}
          {sorted.length === 0 && (
            <tr>
              <td colSpan={5} className="inventory-table__empty">No equipment recorded for this station.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
