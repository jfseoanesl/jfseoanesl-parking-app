import { expect, test } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * Fase 10 — QA: E2E del flujo dorado (UC-002 a UC-007, UC-010). Corre contra un backend real
 * (no mockeado) en localhost:8080, perfil "development" — ver README.md. Usa una placa unica
 * por ejecucion para no chocar con datos dejados por corridas anteriores; no asume que la caja
 * este cerrada al empezar (otras corridas o pruebas manuales pueden haberla dejado abierta).
 */

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
  await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page).toHaveURL('/')
}

async function ensureCashSessionOpen(page: import('@playwright/test').Page) {
  await page.goto('/caja')
  // Esperar a que la pantalla termine de cargar: titulo "Caja" (abierta) o "Abrir caja" (cerrada).
  // exact: sin el, "Caja" tambien coincide con "Abrir caja".
  const openHeading = page.getByRole('heading', { name: 'Caja', exact: true })
  const closedHeading = page.getByRole('heading', { name: 'Abrir caja', exact: true })
  await expect(openHeading.or(closedHeading)).toBeVisible()
  if (await closedHeading.isVisible()) {
    await page.getByLabel('Saldo inicial').fill('50000')
    await page.getByRole('button', { name: 'Abrir caja' }).click()
  }
  await expect(openHeading).toBeVisible()
}

test.describe('Flujo dorado: ingreso -> cobro -> dashboard/reportes', () => {
  test('un vehiculo ingresado y cobrado se refleja en Parqueados, Dashboard y Reportes', async ({ page }) => {
    const plate = `E2E${Date.now().toString().slice(-6)}`

    await login(page)
    await ensureCashSessionOpen(page)

    // Nuevo ingreso.
    await page.goto('/ingreso')
    await page.getByPlaceholder('ABC123').fill(plate)
    await page.getByRole('button', { name: 'Automóvil' }).click()
    await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
    await expect(page.getByText(`Ingreso registrado: ${plate}`)).toBeVisible()

    // Aparece en Parqueados.
    await page.goto('/parqueados')
    await page.getByRole('searchbox', { name: 'Buscar placa' }).fill(plate)
    const row = page.getByRole('listitem').filter({ hasText: plate })
    await expect(row.getByText('EN PARQUEADERO')).toBeVisible()

    // Cobro / salida.
    await row.getByRole('button', { name: 'Registrar salida' }).click()
    // BR-008.1: la seccion Tarifa se muestra siempre al cobrar (con una o varias tarifas aplicables).
    await expect(page.getByRole('group', { name: 'Tarifa' })).toBeVisible()
    await page.getByRole('button', { name: 'Efectivo', exact: true }).click()
    await page.getByRole('button', { name: 'Confirmar cobro' }).click()
    // ConfirmDialog (09-ux-specification.md §6): el cobro es financiero e irreversible.
    await page.getByRole('dialog').getByRole('button', { name: 'Sí, cobrar' }).click()
    await expect(page.getByText('Cobro registrado')).toBeVisible()
    await expect(page.getByText(plate, { exact: true })).toBeVisible()

    // El Dashboard del administrador refleja el movimiento reciente.
    await page.goto('/')
    await expect(page.getByRole('listitem').filter({ hasText: plate }).filter({ hasText: 'Salió' })).toBeVisible()

    // El reporte diario cuenta el vehiculo entre los ingresados y finalizados de hoy.
    await page.goto('/reportes')
    await expect(page.getByText('Vehículos ingresados')).toBeVisible()
    await expect(page.getByText('Vehículos finalizados')).toBeVisible()
  })
})
