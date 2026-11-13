import { useCallback, useEffect, useRef, useState } from 'react'
import { ToastContext } from './context.js'

export function ToastProvider({ children }) {
  const [message, setMessage] = useState('')
  const [visible, setVisible] = useState(false)
  const timer = useRef(0)

  const toast = useCallback((text) => {
    setMessage(text)
    setVisible(true)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setVisible(false), 2600)
  }, [])

  useEffect(() => () => clearTimeout(timer.current), [])

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className={'toast' + (visible ? ' show' : '')} role="status" aria-live="polite">
        {message}
      </div>
    </ToastContext.Provider>
  )
}
