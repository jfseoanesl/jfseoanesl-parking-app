import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vitest/config'

// Config de pruebas separada de vite.config.ts: evita que el plugin PWA (que solo aplica en
// build de produccion) interfiera con Vitest.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    // e2e/ son specs de Playwright (otro test runner, ejecutado via `npm run test:e2e`).
    exclude: ['node_modules/**', 'e2e/**'],
  },
})
