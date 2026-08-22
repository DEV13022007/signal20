import { useMemo, useState } from 'react'
import { useDashboardData } from '../hooks/useDashboardData'
import { StatusBar } from '../components/StatusBar'
import { StationList } from '../components/StationList'
import { StationDetail } from '../components/StationDetail'
import { SyncQueuePanel } from '../components/SyncQueuePanel'
import './Dashboard.css'

export function Dashboard() {
  const {
    loading,
    error,
    servingFromCache,
    stations,
    inventory,
    syncStatus,
    syncRecords,
    simulatedOffline,
    toggleSimulatedOffline,
    refresh,
  } = useDashboardData()
  const [selectedId, setSelectedId] = useState(null)

  const activeId = selectedId ?? stations[0]?.id ?? null
  const selectedStation = stations.find((s) => s.id === activeId) ?? null
  const stationsById = useMemo(() => Object.fromEntries(stations.map((s) => [s.id, s])), [stations])

  const pendingByStation = useMemo(() => {
    const map = {}
    for (const record of syncRecords) {
      if (record.status === 'PENDING') map[record.stationId] = (map[record.stationId] ?? 0) + 1
    }
    return map
  }, [syncRecords])

  const itemsForStation = inventory.filter((item) => item.stationId === activeId)

  const criticalCount = useMemo(() => {
    const failedSyncs = syncRecords.filter((r) => r.status === 'FAILED').length
    const lowStock = inventory.filter((i) => i.minThreshold != null && i.quantity <= i.minThreshold).length
    return failedSyncs + lowStock
  }, [syncRecords, inventory])

  const linkedCount = stations.filter((s) => s.satelliteLinkActive).length

  if (loading) {
    return (
      <div className="dashboard-loading">
        <span className="mono">ESTABLISHING LINK…</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="dashboard-loading dashboard-loading--error">
        <p>Could not reach PolarConnect and no cached data was found.</p>
        <p className="mono">{error}</p>
        <button onClick={refresh}>Retry</button>
      </div>
    )
  }

  return (
    <div className="dashboard">
      <StatusBar
        servingFromCache={servingFromCache}
        linkedCount={linkedCount}
        totalStations={stations.length}
        syncStatus={syncStatus}
        criticalCount={criticalCount}
        simulatedOffline={simulatedOffline}
        onToggleSimulatedOffline={toggleSimulatedOffline}
      />
      <div className="dashboard__body">
        <StationList
          stations={stations}
          pendingByStation={pendingByStation}
          selectedId={activeId}
          onSelect={setSelectedId}
        />
        <StationDetail station={selectedStation} items={itemsForStation} onChanged={refresh} />
        <SyncQueuePanel records={syncRecords} stationsById={stationsById} />
      </div>
    </div>
  )
}
