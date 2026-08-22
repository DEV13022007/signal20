import { useMemo, useState } from 'react'
import { useDashboardData } from '../hooks/useDashboardData'
import { useAlerts } from '../hooks/useAlerts'
import { StatusBar } from '../components/StatusBar'
import { StationList } from '../components/StationList'
import { StationDetail } from '../components/StationDetail'
import { SyncQueuePanel } from '../components/SyncQueuePanel'
import { AlertsPanel } from '../components/AlertsPanel'
import './Dashboard.css'

export function Dashboard() {
  const {
    loading,
    error,
    servingFromCache,
    stations,
    inventory,
    personnel,
    equipment,
    syncStatus,
    syncRecords,
    simulatedOffline,
    toggleSimulatedOffline,
    refresh,
  } = useDashboardData()
  const { alerts, connected: alertsConnected } = useAlerts()
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
  const crewForStation = personnel.filter((person) => person.stationId === activeId)
  const equipmentForStation = equipment.filter((e) => e.stationId === activeId)

  // Sourced from the live alert feed (WebSocket + history), not recomputed locally, so
  // the counter reflects exactly what the alerting module has raised.
  const criticalCount = alerts.length

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
        <StationDetail
          station={selectedStation}
          items={itemsForStation}
          crew={crewForStation}
          equipment={equipmentForStation}
          onChanged={refresh}
        />
        <div className="dashboard__right-rail">
          <SyncQueuePanel records={syncRecords} stationsById={stationsById} />
          <AlertsPanel alerts={alerts} connected={alertsConnected} />
        </div>
      </div>
    </div>
  )
}
