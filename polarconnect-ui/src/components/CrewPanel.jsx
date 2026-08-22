import { useState } from 'react'
import { repo } from '../api/offlineRepo'
import './CrewPanel.css'

const HEALTH_STATUSES = ['NOMINAL', 'MONITORING', 'CRITICAL']
const HEALTH_ORDER = { CRITICAL: 0, MONITORING: 1, NOMINAL: 2 }

function formatDate(dateStr) {
  return dateStr ?? '—'
}

export function CrewPanel({ crew, onChanged }) {
  const [busyId, setBusyId] = useState(null)

  const sorted = [...crew].sort((a, b) => HEALTH_ORDER[a.healthStatus] - HEALTH_ORDER[b.healthStatus])

  async function changeHealth(person, healthStatus) {
    if (healthStatus === person.healthStatus) return
    setBusyId(person.id)
    try {
      await repo.updatePersonnel(person.id, { ...person, healthStatus })
      await onChanged()
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="crew-panel">
      <div className="station-detail__section-title eyebrow">Crew · {sorted.length}</div>
      <table className="crew-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Role</th>
            <th>Rotation</th>
            <th>Health</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((person) => (
            <tr key={person.id} className={person.healthStatus !== 'NOMINAL' ? 'crew-row--alert' : ''}>
              <td>{person.name}</td>
              <td className="mono">{person.role}</td>
              <td className="mono">
                {formatDate(person.rotationStart)} → {formatDate(person.rotationEnd)}
              </td>
              <td>
                <select
                  className={`health-select health-select--${person.healthStatus.toLowerCase()}`}
                  value={person.healthStatus}
                  disabled={busyId === person.id}
                  onChange={(e) => changeHealth(person, e.target.value)}
                >
                  {HEALTH_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
          {sorted.length === 0 && (
            <tr>
              <td colSpan={4} className="inventory-table__empty">No crew recorded for this station.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
