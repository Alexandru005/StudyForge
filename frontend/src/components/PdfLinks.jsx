import { apiUrl } from '../api.js'
import { IconDownload, IconOpen } from './icons.jsx'

/** Deschide / descarcă PDF-ul salvat în backend (pdfUrl și downloadUrl vin din API). */
export default function PdfLinks({ pdfUrl, downloadUrl }) {
  if (!pdfUrl) return null
  return (
    <div className="pdf-links">
      <a className="btn-ghost" href={apiUrl(pdfUrl)} target="_blank" rel="noopener noreferrer">
        <IconOpen />
        Deschide PDF
      </a>
      <a className="btn-ghost" href={apiUrl(downloadUrl || pdfUrl)}>
        <IconDownload />
        Descarcă
      </a>
    </div>
  )
}
