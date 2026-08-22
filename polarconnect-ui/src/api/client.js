import { isSimulatedOffline } from '../lib/simulateOffline'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function request(path, options = {}) {
  if (isSimulatedOffline()) {
    throw new TypeError('Failed to fetch (simulated offline)')
  }
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`${options.method ?? 'GET'} ${path} failed: ${res.status}`)
  }
  return res.status === 204 ? null : res.json()
}

export async function downloadReport(stationId) {
  const res = await fetch(`${BASE_URL}/stations/${stationId}/report`)
  if (!res.ok) {
    throw new Error(`Report generation failed: ${res.status}`)
  }
  const blob = await res.blob()
  const disposition = res.headers.get('Content-Disposition') ?? ''
  const filename = disposition.match(/filename="?([^"]+)"?/)?.[1] ?? `station-${stationId}-report.pdf`

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export const api = {
  getStations: () => request('/stations'),
  setSatelliteLink: (id, active) =>
    request(`/stations/${id}/satellite-link?active=${active}`, { method: 'PATCH' }),

  getInventory: (stationId) =>
    request(stationId ? `/inventory?stationId=${stationId}` : '/inventory'),
  createInventoryItem: (stationId, item) =>
    request(`/inventory?stationId=${stationId}`, { method: 'POST', body: JSON.stringify(item) }),

  getPersonnel: (stationId) =>
    request(stationId ? `/personnel?stationId=${stationId}` : '/personnel'),
  updatePersonnel: (id, person) => request(`/personnel/${id}`, { method: 'PUT', body: JSON.stringify(person) }),

  getEquipment: (stationId) =>
    request(stationId ? `/equipment?stationId=${stationId}` : '/equipment'),
  updateEquipment: (id, equipment) => request(`/equipment/${id}`, { method: 'PUT', body: JSON.stringify(equipment) }),

  getAlerts: () => request('/alerts'),

  getSyncStatus: () => request('/sync/status'),
  getStationSyncStatus: (stationId) => request(`/sync/status/${stationId}`),
  getSyncRecords: (status) => request(status ? `/sync/records?status=${status}` : '/sync/records'),
  flushStation: (stationId) => request(`/sync/stations/${stationId}/flush`, { method: 'POST' }),
}
