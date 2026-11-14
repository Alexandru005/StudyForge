import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './styles/home.css'
import './styles/app.css'
import './styles/pages.css'
import './styles/auth.css'
import './styles/progress.css'
import './styles/friends.css'
import './styles/extras.css'
import './styles/accent.css' // ultimul: culoarea verde de accent suprascrie stilurile de bază
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
