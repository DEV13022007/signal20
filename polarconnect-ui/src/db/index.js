import Dexie from 'dexie'

// Local mirror of the Spring Boot entities (Station, InventoryItem, SyncRecord)
// plus an outbox for writes made while the app itself is offline. The outbox is
// separate from the sync_records the backend already tracks per station: this one
// tracks the browser <-> backend leg, the backend one tracks station <-> HQ.
export const db = new Dexie('polarconnect')

db.version(1).stores({
  stations: 'id, code, satelliteLinkActive',
  inventoryItems: 'id, stationId, priority, expiryDate',
  syncRecords: 'id, stationId, status, priority',
  outbox: '++localId, entity, createdAt',
  meta: 'key',
})

export async function cacheStations(stations) {
  await db.stations.bulkPut(stations)
}

export async function cacheInventoryItems(items) {
  await db.inventoryItems.bulkPut(items)
}

export async function cacheSyncRecords(records) {
  await db.syncRecords.bulkPut(records)
}

export async function queueOutboxWrite(entity, method, url, body) {
  return db.outbox.add({
    entity,
    method,
    url,
    body,
    createdAt: Date.now(),
  })
}

export async function getLastSyncedAt() {
  const row = await db.meta.get('lastSyncedAt')
  return row?.value ?? null
}

export async function setLastSyncedAt(timestamp) {
  await db.meta.put({ key: 'lastSyncedAt', value: timestamp })
}
