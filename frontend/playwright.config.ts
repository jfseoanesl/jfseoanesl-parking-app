import { defineConfig, devices } from '@playwright/test'

/**
 * Fase 10 — QA: E2E y responsive (prompt.md §Fase 10). Requiere el backend corriendo por
 * separado en localhost:8080 (perfil "development", ver README.md) — el webServer de abajo
 * solo levanta el frontend; Playwright reutiliza un servidor ya corriendo si lo encuentra.
 *
 * Contra otro stack (ej. el de produccion con MySQL, http://localhost:8091): E2E_BASE_URL apunta ahi
 * y no se levanta el servidor de desarrollo; credenciales con E2E_ADMIN_USERNAME/_PASSWORD
 * (e2e/credentials.ts). E2E_BROWSER_CHANNEL=chrome usa el Chrome instalado en lugar del Chromium
 * descargado por Playwright.
 */
const externalBaseUrl = process.env.E2E_BASE_URL
const channel = process.env.E2E_BROWSER_CHANNEL

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // los escenarios comparten estado de negocio real contra un backend vivo.
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: externalBaseUrl ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  webServer: externalBaseUrl
    ? undefined
    : {
        command: 'npm run dev -- --port 5173',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        timeout: 30000,
      },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], channel } },
    { name: 'mobile', use: { ...devices['Pixel 7'], channel } },
  ],
})
