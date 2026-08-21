import { api } from './client'
import { db, cacheStations, cacheInventoryItems, cacheSyncRecords, setLastSyncedAt } from '../db'

// Network-first with an IndexedDB fallback: try the backend, cache what comes back,
// and if the fetch throws (offline, satellite link down, DNS blip) serve the last
// cached snapshot instead of a blank screen.
async function withCache(fetcher, cacheWriter, dexieTable) {
  try {
    const data = await fetcher()
    if (cacheWriter) await cacheWriter(data)
    await setLastSyncedAt(Date.now())
    return { data, fromCache: false }
  } catch (err) {
    if (dexieTable) {
      const cached = await dexieTable.toArray()
      if (cached.length) return { data: cached, fromCache: true }
    }
    throw err
  }
}

export const repo = {
  getStations: () => withCache(api.getStations, cacheStations, db.stations),

  getInventory: (stationId) =>
    withCache(
      () => api.getInventory(stationId),
      cacheInventoryItems,
      stationId ? db.inventoryItems.where('stationId').equals(stationId) : db.inventoryItems,
    ),

  getSyncStatus: async () => {
    try {
      const data = await api.getSyncStatus()
      await setLastSyncedAt(Date.now())
      return { data, fromCache: false }
    } catch (err) {
      // Overall status has no direct Dexie table; derive a degraded view from
      // whatever sync records were last cached instead of failing outright.
      const cached = await db.syncRecords.toArray()
      if (cached.length) {
        const pending = cached.filter((r) => r.status === 'PENDING')
        const pendingByPriority = pending.reduce((acc, r) => {
          acc[r.priority] = (acc[r.priority] ?? 0) + 1
          return acc
        }, {})
        return {
          data: {
            totalPending: pending.length,
            totalSynced: cached.filter((r) => r.status === 'SYNCED').length,
            pendingByPriority,
            stations: [],
          },
          fromCache: true,
        }
      }
      throw err
    }
  },

  getSyncRecords: (status) => withCache(() => api.getSyncRecords(status), cacheSyncRecords, db.syncRecords),

  setSatelliteLink: api.setSatelliteLink,
  flushStation: api.flushStation,
}
