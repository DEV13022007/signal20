// Real network loss can't be triggered from within the app, so the demo needs a manual
// switch: flipping this makes every request/api call fail exactly as if the device had
// lost its link, without touching the OS network stack.
let simulated = false
const listeners = new Set()

export function isSimulatedOffline() {
  return simulated
}

export function setSimulatedOffline(value) {
  if (simulated === value) return
  simulated = value
  listeners.forEach((fn) => fn(simulated))
}

export function subscribeSimulatedOffline(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
