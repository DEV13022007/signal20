import { useCallback, useEffect, useState } from 'react'
import { isSimulatedOffline, setSimulatedOffline, subscribeSimulatedOffline } from '../lib/simulateOffline'

export function useOnlineStatus() {
  const [browserOnline, setBrowserOnline] = useState(navigator.onLine)
  const [simulatedOffline, setSimulatedOfflineState] = useState(isSimulatedOffline())

  useEffect(() => {
    const goOnline = () => setBrowserOnline(true)
    const goOffline = () => setBrowserOnline(false)
    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
    }
  }, [])

  useEffect(() => subscribeSimulatedOffline(setSimulatedOfflineState), [])

  const toggleSimulatedOffline = useCallback(() => {
    setSimulatedOffline(!isSimulatedOffline())
  }, [])

  return {
    isOnline: browserOnline && !simulatedOffline,
    simulatedOffline,
    toggleSimulatedOffline,
  }
}
