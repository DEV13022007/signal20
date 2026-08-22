import { useState } from 'react'
import { repo } from '../api/offlineRepo'
import { downloadReport } from '../api/client'
import { PRIORITIES, PRIORITY_ORDER } from '../constants/priority'
import { CrewPanel } from './CrewPanel'
import { EquipmentPanel } from './EquipmentPanel'
import './StationDetail.css'

const EXPIRY_WARNING_DAYS = 90

const EMPTY_FORM = { name: '', priority: 'ROUTINE', quantity: '', unit: '', minThreshold: '', expiryDate: '' }

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

export function StationDetail({ station, items, crew, equipment, onChanged }) {
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [note, setNote] = useState(null)
  const [reportNote, setReportNote] = useState(null)

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

  async function generateReport() {
    setBusy(true)
    setReportNote(null)
    try {
      await downloadReport(station.id)
    } catch (err) {
      setReportNote(`Could not generate report: ${err.message}`)
    } finally {
      setBusy(false)
    }
  }

  function updateField(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function addItem(e) {
    e.preventDefault()
    if (!form.name.trim() || form.quantity === '') return
    setBusy(true)
    try {
      const { queued } = await repo.createInventoryItem(station.id, {
        name: form.name.trim(),
        priority: form.priority,
        quantity: Number(form.quantity),
        unit: form.unit.trim() || null,
        minThreshold: form.minThreshold === '' ? null : Number(form.minThreshold),
        expiryDate: form.expiryDate || null,
      })
      setNote(
        queued
          ? `"${form.name.trim()}" queued locally — will sync when the connection returns.`
          : `"${form.name.trim()}" added and synced.`,
      )
      setForm(EMPTY_FORM)
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
          <div className="station-detail__meta mono">
            Capacity {station.capacity ?? '—'} · {station.currentSeason ?? '—'} season · Est. {station.operationalSinceYear ?? '—'}
          </div>
        </div>
        <div className="station-detail__actions">
          <span className={`link-state ${station.satelliteLinkActive ? 'link-state--up' : 'link-state--down'}`}>
            {station.satelliteLinkActive ? 'LINK ACTIVE' : 'LINK DOWN'}
          </span>
          <button
            className={`link-toggle ${station.satelliteLinkActive ? 'link-toggle--up' : 'link-toggle--down'}`}
            onClick={toggleLink}
            disabled={busy}
          >
            {station.satelliteLinkActive ? 'Deactivate link' : 'Activate link'}
          </button>
          <button className="flush-btn" onClick={flush} disabled={busy}>
            Flush sync queue
          </button>
          <button className="report-btn" onClick={generateReport} disabled={busy}>
            Generate report
          </button>
        </div>
      </div>
      {reportNote && <div className="add-item-form__note">{reportNote}</div>}

      <div className="station-detail__section-title eyebrow">Inventory · {sorted.length} items</div>

      <form className="add-item-form" onSubmit={addItem}>
        <input
          className="add-item-form__input"
          placeholder="Item name"
          value={form.name}
          onChange={updateField('name')}
          required
        />
        <select className="add-item-form__select" value={form.priority} onChange={updateField('priority')}>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
        <input
          className="add-item-form__input add-item-form__input--qty"
          type="number"
          min="0"
          placeholder="Qty"
          value={form.quantity}
          onChange={updateField('quantity')}
          required
        />
        <input
          className="add-item-form__input add-item-form__input--unit"
          placeholder="Unit"
          value={form.unit}
          onChange={updateField('unit')}
        />
        <input
          className="add-item-form__input add-item-form__input--qty"
          type="number"
          min="0"
          placeholder="Min threshold"
          value={form.minThreshold}
          onChange={updateField('minThreshold')}
        />
        <input
          className="add-item-form__input"
          type="date"
          value={form.expiryDate}
          onChange={updateField('expiryDate')}
        />
        <button className="add-item-form__submit" type="submit" disabled={busy}>
          Add item
        </button>
      </form>
      {note && <div className="add-item-form__note">{note}</div>}

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

      <CrewPanel crew={crew} onChanged={onChanged} />
      <EquipmentPanel equipment={equipment} onChanged={onChanged} />
    </div>
  )
}
