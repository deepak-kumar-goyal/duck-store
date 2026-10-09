import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

const DEFAULT_API_URL = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // VITE_API_URL (see .env.example) points the dev server at a backend running elsewhere.
  const env = loadEnv(mode, '.', 'VITE_')
  return {
    plugins: [react()],
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.js',
    },
    server: {
      // Proxying /api avoids CORS configuration in development.
      proxy: { '/api': env.VITE_API_URL || DEFAULT_API_URL },
    },
  }
})
