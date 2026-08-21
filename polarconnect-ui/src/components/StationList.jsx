import './StationList.css'

export function StationList({ stations, pendingByStation, selectedId, onSelect }) {
  return (
    <nav className="station-list" aria-label="Stations">
      <div className="eyebrow station-list__heading">Stations · {stations.length}</div>
      <ul>
        {stations.map((station) => {
          const pending = pendingByStation[station.id] ?? 0
          return (
            <li key={station.id}>
              <button
                className={`station-row ${station.id === selectedId ? 'station-row--active' : ''}`}
                onClick={() => onSelect(station.id)}
                aria-current={station.id === selectedId}
              >
                <span
                  className={`signal ${station.satelliteLinkActive ? 'signal--up' : 'signal--down'}`}
                  aria-hidden="true"
                >
                  <span />
                  <span />
                  <span />
                </span>
                <span className="station-row__name">
                  <span className="station-row__code mono">{station.code}</span>
                  <span className="station-row__full">{station.name}</span>
                </span>
                {pending > 0 && <span className="station-row__badge mono">{pending}</span>}
              </button>
            </li>
          )
        })}
        {stations.length === 0 && <li className="station-list__empty">No stations registered yet.</li>}
      </ul>
    </nav>
  )
}
