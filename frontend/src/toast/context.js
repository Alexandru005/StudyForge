import { createContext, useContext } from 'react'

export const ToastContext = createContext(() => {})

/** const toast = useToast(); toast('Mesaj'); */
export function useToast() {
  return useContext(ToastContext)
}
