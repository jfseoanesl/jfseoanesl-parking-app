import { expect, test } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * Fase 10 — QA: pruebas responsive (RNF-006). Corre unicamente en el proyecto "mobile"
 * (playwright.config.ts, viewport de Pixel 7) para verificar en un viewport real de telefono
 * lo que en la Fase 9 no se pudo confirmar visualmente (la herramienta de redimensionado del
 * navegador conectado no aplicaba el tamaño solicitado - ver specs/00-product-vision.md Fase 9).
 */

test.describe('Responsive (solo proyecto mobile)', () => {
  test.skip(({ isMobile }) => !isMobile, 'Este archivo solo aplica al viewport movil (Pixel 7).')

  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
    await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page).toHaveURL('/')
  })

  const routes = ['/', '/parqueados', '/ingreso', '/caja', '/reportes', '/usuarios', '/tarifas', '/configuracion', '/historial', '/auditoria', '/respaldo']

  for (const route of routes) {
    test(`${route} no produce scroll horizontal en viewport movil`, async ({ page }) => {
      await page.goto(route)
      const { scrollWidth, clientWidth } = await page.evaluate(() => ({
        scrollWidth: document.documentElement.scrollWidth,
        clientWidth: document.documentElement.clientWidth,
      }))
      expect(scrollWidth).toBeLessThanOrEqual(clientWidth + 1) // +1: redondeo de subpixeles.
    })
  }

  test('la barra inferior movil reemplaza a la barra lateral de escritorio', async ({ page }) => {
    await page.goto('/')
    // Sidebar ("hidden md:block"): oculta por debajo del breakpoint md; solo aparece ahi.
    const sidebar = page.locator('nav.hidden.md\\:block')
    await expect(sidebar.getByRole('link', { name: 'Usuarios' })).toBeHidden()
    // BottomNav (wrapper "md:hidden"): visible por debajo del breakpoint md.
    const bottomNav = page.locator('div.md\\:hidden')
    await expect(bottomNav.getByRole('link', { name: 'Reportes' })).toBeVisible()
  })

  test('los objetivos tactiles de la barra inferior tienen al menos 44px de alto (RNF-004)', async ({ page }) => {
    await page.goto('/')
    const homeLink = page.getByRole('link', { name: 'Dashboard' }).last()
    const box = await homeLink.boundingBox()
    expect(box?.height ?? 0).toBeGreaterThanOrEqual(44)
  })
})
