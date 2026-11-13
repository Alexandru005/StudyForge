import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  // Toate apelurile /api/* merg către backend-ul Spring Boot (implicit pe 8080).
  // Generarea cu Gemini poate dura minute, de aceea timeout-uri mari.
  const proxy = {
    '/api': {
      target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
      timeout: 600000,
      proxyTimeout: 600000,
    },
  }
  return {
    plugins: [react()],
    server: { proxy },
    preview: { proxy },
  }
})
