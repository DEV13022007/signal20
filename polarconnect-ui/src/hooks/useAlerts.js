import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { api } from '../api/client'

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

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body)
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
