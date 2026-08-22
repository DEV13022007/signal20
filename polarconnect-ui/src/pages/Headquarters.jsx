import { Link } from 'react-router-dom'
import { useDashboardData } from '../hooks/useDashboardData'
import { useAlerts } from '../hooks/useAlerts'
import { useClock, formatUtc } from '../hooks/useClock'
import { useAuth } from '../hooks/useAuth'
import './Headquarters.css'

const FUEL_KEYWORDS = ['fuel', 'diesel', 'petrol', 'kerosene']
const FOOD_KEYWORDS = ['rice', 'milk', 'vegetable', 'meat', 'grain', 'flour', 'sugar', 'wheat', 'lentil']

function matchesAny(name, keywords) {
  const lower = name.toLowerCase()
  return keywords.some((k) => lower.includes(k))
}

function positionSummary(items, keywords) {
  const matches = items.filter((i) => matchesAny(i.name, keywords))
  if (matches.length === 0) return '—'
  return matches.map((i) => `${i.name} ${i.quantity}${i.unit ? ` ${i.unit}` : ''}`).join(', ')
}

function timeAgo(iso) {
  if (!iso) return 'Never'
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000))
  if (seconds < 60) return `${seconds}s ago`
  if (seconds < 3600) return `${Math.round(seconds / 60)}m ago`
  return `${Math.round(seconds / 3600)}h ago`
}

export function Headquarters() {
  const { loading, stations, inventory, syncRecords } = useDashboardData()
  const { alerts } = useAlerts()
  const { user, logout } = useAuth()
  const now = useClock()

  if (loading) {
    return (
      <div className="hq-loading">
        <span className="mono">ESTABLISHING LINK…</span>
      </div>
    )
  }

  return (
    <div className="hq">
      <header className="hq__header">
        <div>
          <div className="eyebrow">POLARCONNECT · INDIA</div>
          <h1 className="hq__title">Headquarters</h1>
        </div>
        <div className="hq__clock mono">{formatUtc(now)}</div>
        {user && <span className="session-info">{user.username} · {user.role.replaceAll('_', ' ')}</span>}
        <Link className="hq__back" to="/">
          ← Station view
        </Link>
        <button className="logout-btn" onClick={logout} type="button">
          Log out
        </button>
      </header>

      <div className="hq__grid">
        {stations.map((station) => {
          const stationRecords = syncRecords.filter((r) => r.stationId === station.id)
          const pending = stationRecords.filter((r) => r.status === 'PENDING').length
          const synced = stationRecords.filter((r) => r.status === 'SYNCED' && r.syncedAt)
          const lastSyncedAt = synced.length
            ? synced.reduce((latest, r) => (r.syncedAt > latest ? r.syncedAt : latest), synced[0].syncedAt)
            : null
          const stationAlerts = alerts.filter((a) => a.stationId === station.id)
          const stationInventory = inventory.filter((i) => i.stationId === station.id)

          return (
            <div key={station.id} className="hq-card">
              <div className="hq-card__header">
                <div>
                  <div className="eyebrow">{station.code} · {station.country ?? 'Unknown'}</div>
                  <h2 className="hq-card__name">{station.name}</h2>
                </div>
                <span className={`link-state ${station.satelliteLinkActive ? 'link-state--up' : 'link-state--down'}`}>
                  {station.satelliteLinkActive ? 'LINK ACTIVE' : 'LINK DOWN'}
                </span>
              </div>

              <dl className="hq-card__stats">
                <div className="hq-stat">
                  <dt>Last synced</dt>
                  <dd className="mono">{timeAgo(lastSyncedAt)}</dd>
                </div>
                <div className="hq-stat">
                  <dt>Critical alerts</dt>
                  <dd className={`mono ${stationAlerts.length > 0 ? 'hq-stat__value--alert' : ''}`}>
                    {stationAlerts.length}
                  </dd>
                </div>
                <div className="hq-stat">
                  <dt>Pending queue depth</dt>
                  <dd className="mono">{pending}</dd>
                </div>
              </dl>

              <div className="hq-card__position">
                <div className="hq-position">
                  <dt>Fuel position</dt>
                  <dd>{positionSummary(stationInventory, FUEL_KEYWORDS)}</dd>
                </div>
                <div className="hq-position">
                  <dt>Food position</dt>
                  <dd>{positionSummary(stationInventory, FOOD_KEYWORDS)}</dd>
                </div>
              </div>
            </div>
          )
        })}
        {stations.length === 0 && <p className="hq__empty">No stations registered yet.</p>}
      </div>
    </div>
  )
}
