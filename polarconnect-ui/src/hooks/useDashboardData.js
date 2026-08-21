import { useCallback, useEffect, useState } from 'react'
import { repo } from '../api/offlineRepo'
import { useOnlineStatus } from './useOnlineStatus'

const POLL_INTERVAL_MS = 30_000

export function useDashboardData() {
  const isOnline = useOnlineStatus()
  const [state, setState] = useState({
    loading: true,
    error: null,
    servingFromCache: false,
    stations: [],
    inventory: [],
    syncStatus: null,
    syncRecords: [],
  })

  const load = useCallback(async () => {
    try {
      const [stationsRes, inventoryRes, syncRes, recordsRes] = await Promise.all([
        repo.getStations(),
        repo.getInventory(),
        repo.getSyncStatus(),
        repo.getSyncRecords(),
      ])
      setState({
        loading: false,
        error: null,
        servingFromCache: stationsRes.fromCache || inventoryRes.fromCache || syncRes.fromCache,
        stations: stationsRes.data,
        inventory: inventoryRes.data,
        syncStatus: syncRes.data,
        syncRecords: recordsRes.data,
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

  // Reconnecting is exactly when a stale cached view is most likely to be wrong.
  useEffect(() => {
    if (isOnline) load()
  }, [isOnline, load])

  return { ...state, isOnline, refresh: load }
}
