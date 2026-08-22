import { useCallback, useEffect, useRef, useState } from 'react'
import { repo, getOutboxAsRecords } from '../api/offlineRepo'
import { useOnlineStatus } from './useOnlineStatus'

const POLL_INTERVAL_MS = 30_000

export function useDashboardData() {
  const { isOnline, simulatedOffline, toggleSimulatedOffline } = useOnlineStatus()
  const [state, setState] = useState({
    loading: true,
    error: null,
    servingFromCache: false,
    stations: [],
    inventory: [],
    personnel: [],
    equipment: [],
    syncStatus: null,
    syncRecords: [],
  })
  const wasOnline = useRef(isOnline)
  const draining = useRef(false)

  const load = useCallback(async () => {
    try {
      const [stationsRes, inventoryRes, personnelRes, equipmentRes, syncRes, recordsRes, outboxRecords] =
        await Promise.all([
          repo.getStations(),
          repo.getInventory(),
          repo.getPersonnel(),
          repo.getEquipment(),
          repo.getSyncStatus(),
          repo.getSyncRecords(),
          getOutboxAsRecords(),
        ])
      setState({
        loading: false,
        error: null,
        servingFromCache: stationsRes.fromCache || inventoryRes.fromCache || syncRes.fromCache,
        stations: stationsRes.data,
        inventory: inventoryRes.data,
        personnel: personnelRes.data,
        equipment: equipmentRes.data,
        syncStatus: syncRes.data,
        syncRecords: [...outboxRecords, ...recordsRes.data],
      })
    } catch (err) {
      setState((prev) => ({ ...prev, loading: false, error: err.message }))
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!isOnline) return
    const id = setInterval(load, POLL_INTERVAL_MS)
    return () => clearInterval(id)
  }, [isOnline, load])

  // Reconnecting is exactly when a stale cached view is most likely to be wrong, and
  // exactly when anything queued in the offline outbox needs to be replayed.
  useEffect(() => {
    if (isOnline && !wasOnline.current && !draining.current) {
      draining.current = true
      repo
        .drainOutbox(() => load())
        .finally(() => {
          draining.current = false
          load()
        })
    } else if (isOnline) {
      load()
    }
    wasOnline.current = isOnline
  }, [isOnline, load])

  return { ...state, isOnline, simulatedOffline, toggleSimulatedOffline, refresh: load }
}
