import { api, request } from './client'
import {
  db,
  cacheStations,
  cacheInventoryItems,
  cacheSyncRecords,
  setLastSyncedAt,
  queueOutboxWrite,
  getOutbox,
  removeOutboxEntry,
} from '../db'
import { PRIORITY_ORDER } from '../constants/priority'

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

function localId() {
  return `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

// Local outbox entries rendered as pseudo sync-records so the queue rail shows them
// PENDING even though the backend has never seen these writes.
export async function getOutboxAsRecords() {
  const entries = await getOutbox()
  return entries.map((entry) => ({
    id: `outbox-${entry.localId}`,
    outboxLocalId: entry.localId,
    stationId: entry.stationId,
    entityType: entry.entity,
    entityId: '—',
    operation: 'CREATE',
    priority: entry.priority,
    status: 'PENDING',
    createdAt: new Date(entry.createdAt).toISOString(),
    syncedAt: null,
    retryCount: 0,
    local: true,
  }))
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

  // Offline-aware write: try the backend first. If the request fails (simulated or
  // real network loss), the write is queued in the Dexie outbox instead of being lost,
  // and an optimistic local copy is cached so it shows up immediately in the UI.
  createInventoryItem: async (stationId, item) => {
    try {
      const created = await api.createInventoryItem(stationId, item)
      await db.inventoryItems.put(created)
      return { data: created, queued: false }
    } catch {
      const id = localId()
      const optimistic = { ...item, id, stationId, local: true }
      await queueOutboxWrite('InventoryItem', 'POST', `/inventory?stationId=${stationId}`, item, item.priority, stationId)
      await db.inventoryItems.put(optimistic)
      return { data: optimistic, queued: true }
    }
  },

  // Replays queued outbox writes in priority order (MEDICAL first, ROUTINE last) once
  // the connection is back. Sequential + awaited so drain order is observable in the
  // sync queue rail rather than firing every request in one indistinguishable burst.
  drainOutbox: async (onEachDrained) => {
    const entries = await getOutbox()
    const ordered = [...entries].sort(
      (a, b) => PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority] || a.createdAt - b.createdAt,
    )

    const drained = []
    for (const entry of ordered) {
      try {
        const created = await request(entry.url, { method: entry.method, body: JSON.stringify(entry.body) })
        // Replace the optimistic local record with the server-confirmed one.
        const optimisticMatches = await db.inventoryItems
          .filter((i) => i.local && i.stationId === entry.stationId && i.name === entry.body.name)
          .toArray()
        for (const m of optimisticMatches) await db.inventoryItems.delete(m.id)
        await db.inventoryItems.put(created)
        await removeOutboxEntry(entry.localId)
        drained.push(entry)
        onEachDrained?.(entry)
        await new Promise((resolve) => setTimeout(resolve, 350))
      } catch {
        // Still offline or backend rejected it — leave it queued and stop this pass;
        // remaining entries will be retried on the next drain trigger.
        break
      }
    }
    return drained
  },
}
