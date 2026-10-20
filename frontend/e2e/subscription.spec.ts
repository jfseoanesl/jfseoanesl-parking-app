import { expect, test, type Page } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * RF-074 a RF-077: crear mensualidad -> cobrarla en caja -> ingreso reconocido -> salida sin cobro
 * -> Historial la muestra como Mensual. Solo desktop: una ejecución basta y usa placas únicas.
 */
async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
  await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Iniciar sesión' }).click()
  await expect(page).toHaveURL('/')
}

async function ensureCashSessionOpen(page: Page) {
  await page.goto('/caja')
  const openHeading = page.getByRole('heading', { name: 'Caja', exact: true })
  const closedHeading = page.getByRole('heading', { name: 'Abrir caja', exact: true })
  await expect(openHeading.or(closedHeading)).toBeVisible()
  if (await closedHeading.isVisible()) {
    await page.getByLabel('Saldo inicial').fill('50000')
    await page.getByRole('button', { name: 'Abrir caja' }).click()
  }
  await expect(openHeading).toBeVisible()
}

test.describe('Mensualidades', () => {
  test.skip(({ isMobile }) => isMobile, 'Una sola ejecución (desktop).')

  test('cobro mensual, ingreso reconocido y salida sin cobro', async ({ page }) => {
    const suffix = Date.now().toString().slice(-5)
    const plate = `MEN${suffix}`
    const customer = `Cliente E2E ${suffix}`

    await login(page)
    await ensureCashSessionOpen(page)

    // Crear la mensualidad.
    await page.goto('/mensualidades')
    await page.getByRole('button', { name: 'Nueva mensualidad' }).click()
    await page.getByLabel('Cliente').fill(customer)
    await page.getByLabel('Valor mensual').fill('90000')
    await page.getByRole('textbox', { name: 'Placa 1', exact: true }).fill(plate)
    await page.getByRole('button', { name: 'Crear mensualidad' }).click()
    await expect(page.getByText(`Mensualidad de ${customer} creada`)).toBeVisible()

    const card = page.getByRole('listitem').filter({ hasText: customer })
    await expect(card.getByText('Sin pago', { exact: true })).toBeVisible()

    // Cobrarla en caja: lleva al comprobante.
    await card.getByRole('button', { name: 'Registrar pago' }).click()
    await page.getByRole('dialog').getByRole('button', { name: /^Cobrar/ }).click()
    const receipt = page.getByRole('article', { name: 'Comprobante de mensualidad' })
    await expect(receipt).toBeVisible()
    await expect(receipt.getByText(customer)).toBeVisible()
    await expect(receipt.getByText(/OP-\d{6}/)).toBeVisible()

    // Ingreso con la placa autorizada: se reconoce como cliente mensual.
    await page.goto('/ingreso')
    await page.getByPlaceholder('ABC123').fill(plate)
    await page.getByRole('button', { name: 'Automóvil' }).click()
    await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
    await expect(page.getByText(`Cliente mensual: ${customer}`)).toBeVisible()

    // Salida cubierta: sin valor a cobrar ni medio de pago.
    await page.goto('/parqueados')
    await page.getByRole('searchbox', { name: 'Buscar placa' }).fill(plate)
    const row = page.getByRole('listitem').filter({ hasText: plate })
    await expect(row.getByText(`Mensual · ${customer}`)).toBeVisible()
    await row.getByRole('button', { name: 'Registrar salida' }).click()
    await expect(page.getByText('Cubierto por mensualidad')).toBeVisible()
    await page.getByRole('button', { name: 'Registrar salida sin cobro' }).click()
    await expect(page.getByText('Salida registrada')).toBeVisible()

    // Historial: finalizada como Mensual, sin comprobante de pago.
    await page.goto('/historial')
    await page.getByLabel('Placa').fill(plate)
    await page.getByRole('button', { name: 'Buscar' }).click()
    const item = page.getByRole('listitem').filter({ hasText: plate })
    await expect(item.getByText('FINALIZADO')).toBeVisible()
    await expect(item.getByText(`Mensual · ${customer}`)).toBeVisible()
    await expect(item.getByRole('button', { name: 'Ver comprobante' })).toHaveCount(0)
  })

  test('reporte de mensualidades con filtros y exportación a Excel', async ({ page }) => {
    const suffix = Date.now().toString().slice(-5)
    const plate = `RPE${suffix}`
    const customer = `Reporte E2E ${suffix}`

    await login(page)
    await page.goto('/mensualidades')
    await page.getByRole('button', { name: 'Nueva mensualidad' }).click()
    await page.getByLabel('Cliente').fill(customer)
    await page.getByLabel('Valor mensual').fill('75000')
    await page.getByRole('textbox', { name: 'Placa 1', exact: true }).fill(plate)
    await page.getByRole('button', { name: 'Crear mensualidad' }).click()
    await expect(page.getByText(`Mensualidad de ${customer} creada`)).toBeVisible()

    await page.goto('/reportes')
    await page.getByRole('group', { name: 'Tipo de reporte' }).getByRole('button', { name: 'Mensualidades' }).click()
    await expect(page.getByRole('heading', { name: 'Reporte de mensualidades' })).toBeVisible()
    await page.getByRole('searchbox', { name: 'Buscar en el reporte' }).fill(plate.toLowerCase())
    const row = page.getByRole('row').filter({ hasText: customer })
    await expect(row).toBeVisible()
    await expect(row.getByText('Sin pago', { exact: true })).toBeVisible()

    // Filtro de estado: "Al día" la excluye; "Sin pago" la vuelve a mostrar.
    const statuses = page.getByRole('group', { name: 'Estados' })
    await statuses.getByRole('button', { name: 'Al día' }).click()
    await expect(page.getByText('Ninguna mensualidad coincide con los filtros')).toBeVisible()
    await statuses.getByRole('button', { name: 'Sin pago' }).click()
    await expect(row).toBeVisible()

    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: 'Excel' }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/^reporte-mensualidades_.*\.xlsx$/)
  })
})
