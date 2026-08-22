import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { api } from '../api/client'
import { getAuthToken, getAuthUser } from '../lib/authStore'

const ALERTS_LIMIT = 100

export function useAlerts() {
  const [alerts, setAlerts] = useState([])
  const [connected, setConnected] = useState(false)
  const clientRef = useRef(null)

  useEffect(() => {
    api
      .getAlerts()
      .then((history) => setAlerts(history))
      .catch(() => {})

    // The broadcast topic isn't station-scoped server-side, so a STATION_MANAGER/CREW
    // client filters out alerts for other stations itself (the REST history above is
    // already scoped by AuthorizationSupport on the backend).
    const user = getAuthUser()
    const scopedStationId = user && user.role !== 'HQ_ADMIN' ? user.stationId : null

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${getAuthToken() ?? ''}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body)
          if (scopedStationId != null && alert.stationId !== scopedStationId) return
          setAlerts((prev) => [alert, ...prev].slice(0, ALERTS_LIMIT))
        })
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    })
    clientRef.current = client
    client.activate()

    return () => {
      client.deactivate()
    }
  }, [])

  return { alerts, connected }
}
