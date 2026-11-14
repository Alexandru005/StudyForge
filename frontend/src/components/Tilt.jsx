import { useRef } from 'react'
import { useTilt } from '../hooks/useTilt.js'

/** Container care se înclină 3D după mouse (aceeași animație ca pe pagina principală). */
export default function Tilt({ max = 8, className, children, as: Tag = 'div', ...rest }) {
  const ref = useRef(null)
  useTilt(ref, { max })
  return (
    <Tag ref={ref} className={className} {...rest}>
      {children}
    </Tag>
  )
}
