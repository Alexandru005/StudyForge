/* Iconițe din design (stroke = culoarea textului curent). */
const base = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

export const IconLogo = () => (
  <svg width="22" height="22" {...base} stroke="#F7D6DC" strokeWidth="2.2">
    <path d="M12 3c1 3 4 4.5 4 8a4 4 0 0 1-8 0c0-1.5.6-2.4 1.5-3.5C10 8.5 11.5 6.5 12 3z" />
    <path d="M5 21h14" />
  </svg>
)

export const IconWarn = ({ size = 18 }) => (
  <svg width={size} height={size} {...base} strokeWidth="2">
    <path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
    <path d="M12 9v4M12 17h.01" />
  </svg>
)

export const IconUpload = () => (
  <svg width="26" height="26" {...base} strokeWidth="2">
    <path d="M12 16V4M7 9l5-5 5 5" />
    <path d="M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" />
  </svg>
)

export const IconSave = () => (
  <svg width="16" height="16" {...base} strokeWidth="2">
    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
    <path d="M17 21v-8H7v8M7 3v5h8" />
  </svg>
)

export const IconSaved = () => (
  <svg width="12" height="12" {...base} strokeWidth="3">
    <path d="M5 12l5 5L20 7" />
  </svg>
)

export const IconCheck = ({ size = 26 }) => (
  <svg width={size} height={size} {...base} strokeWidth="3">
    <path d="M5 12.5l4.5 4.5L19 7.5" />
  </svg>
)

export const IconCross = () => (
  <svg width="24" height="24" {...base} strokeWidth="3">
    <path d="M6 6l12 12M18 6L6 18" />
  </svg>
)

export const IconFlip = () => (
  <svg width="16" height="16" {...base} strokeWidth="2">
    <path d="M21 12a9 9 0 1 1-3-6.7L21 8" />
    <path d="M21 3v5h-5" />
  </svg>
)

export const IconFlipBack = () => (
  <svg width="16" height="16" {...base} strokeWidth="2">
    <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
    <path d="M3 3v5h5" />
  </svg>
)

export const IconPrev = () => (
  <svg width="16" height="16" {...base} strokeWidth="2.4">
    <path d="M15 6l-6 6 6 6" />
  </svg>
)

export const IconNext = () => (
  <svg width="16" height="16" {...base} strokeWidth="2.4">
    <path d="M9 6l6 6-6 6" />
  </svg>
)

export const IconOpen = () => (
  <svg width="16" height="16" {...base} strokeWidth="2">
    <path d="M14 3h7v7M21 3l-9 9" />
    <path d="M19 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5" />
  </svg>
)

export const IconDownload = () => (
  <svg width="16" height="16" {...base} strokeWidth="2">
    <path d="M12 4v12M7 11l5 5 5-5" />
    <path d="M4 20h16" />
  </svg>
)

export const IconArrow = () => (
  <svg width="20" height="20" {...base} strokeWidth="2">
    <path d="M5 12h14M13 6l6 6-6 6" />
  </svg>
)

export const IconLogin = () => (
  <svg width="24" height="24" {...base} strokeWidth="2">
    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3" />
  </svg>
)

export const IconUserPlus = () => (
  <svg width="24" height="24" {...base} strokeWidth="2">
    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M19 8v6M22 11h-6" />
  </svg>
)

export const IconLogout = () => (
  <svg width="18" height="18" {...base} strokeWidth="2">
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
  </svg>
)

export const IconChevron = () => (
  <svg width="14" height="14" {...base} strokeWidth="2.5">
    <path d="M6 9l6 6 6-6" />
  </svg>
)
