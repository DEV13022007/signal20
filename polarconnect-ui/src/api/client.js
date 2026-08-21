const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    throw new Error(`${options.method ?? 'GET'} ${path} failed: ${res.status}`)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  getStations: () => request('/stations'),
  setSatelliteLink: (id, active) =>
    request(`/stations/${id}/satellite-link?active=${active}`, { method: 'PATCH' }),

  getInventory: (stationId) =>
    request(stationId ? `/inventory?stationId=${stationId}` : '/inventory'),

  getSyncStatus: () => request('/sync/status'),
  getStationSyncStatus: (stationId) => request(`/sync/status/${stationId}`),
  getSyncRecords: (status) => request(status ? `/sync/records?status=${status}` : '/sync/records'),
  flushStation: (stationId) => request(`/sync/stations/${stationId}/flush`, { method: 'POST' }),
}
