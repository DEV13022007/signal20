import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Dashboard } from './pages/Dashboard'
import { Headquarters } from './pages/Headquarters'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/hq" element={<Headquarters />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
