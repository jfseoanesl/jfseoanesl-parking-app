import { expect, test, type Page } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * Cierre de brechas funcionales: comprobante imprimible (RF-050), operaciones del turno (RF-059),
 * historial (RF-058), anulación administrativa (RF-068) y filtros de auditoría (RF-067).
 * Corre contra un backend real; cada prueba usa una placa única.
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

async function registerEntry(page: Page, plate: string) {
  await page.goto('/ingreso')
  await page.getByPlaceholder('ABC123').fill(plate)
  await page.getByRole('button', { name: 'Automóvil' }).click()
  await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
  await expect(page.getByText(`Ingreso registrado: ${plate}`)).toBeVisible()
}

function uniquePlate(prefix: string) {
  return `${prefix}${Date.now().toString().slice(-5)}`
}

test.describe('Funcionalidades administrativas completas', () => {
  test('cobro -> comprobante imprimible -> operaciones del turno -> historial', async ({ page }) => {
    const plate = uniquePlate('CMP')
    await login(page)
    await ensureCashSessionOpen(page)
    await registerEntry(page, plate)

    await page.goto('/parqueados')
    await page.getByRole('searchbox', { name: 'Buscar placa' }).fill(plate)
    await page.getByRole('listitem').filter({ hasText: plate }).getByRole('button', { name: 'Registrar salida' }).click()
    await page.getByRole('button', { name: 'Efectivo', exact: true }).click()
    await page.getByRole('button', { name: 'Confirmar cobro' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Sí, cobrar' }).click()
    await expect(page.getByText('Cobro registrado')).toBeVisible()

    // RF-050: comprobante imprimible.
    await page.getByRole('button', { name: 'Ver comprobante para imprimir' }).click()
    const receipt = page.getByRole('article', { name: 'Comprobante de pago' })
    await expect(receipt).toBeVisible()
    await expect(receipt.getByText(plate)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Imprimir' })).toBeVisible()

    // RF-059: la operación aparece en "Operaciones de mi turno".
    await page.goto('/caja')
    await expect(page.getByRole('listitem').filter({ hasText: plate })).toBeVisible()

    // RF-058: el historial la muestra como finalizada, con acceso al comprobante.
    await page.goto('/historial')
    await page.getByLabel('Placa').fill(plate)
    await page.getByRole('button', { name: 'Buscar' }).click()
    const item = page.getByRole('listitem').filter({ hasText: plate })
    await expect(item.getByText('FINALIZADO')).toBeVisible()
    await expect(item.getByRole('button', { name: 'Ver comprobante' })).toBeVisible()
  })

  test('anulación administrativa con motivo -> historial y auditoría', async ({ page }) => {
    const plate = uniquePlate('ANU')
    const reason = `Ingreso duplicado ${plate}`
    await login(page)
    await registerEntry(page, plate)

    // RF-068: el Administrador anula desde Parqueados.
    await page.goto('/parqueados')
    await page.getByRole('searchbox', { name: 'Buscar placa' }).fill(plate)
    await page.getByRole('listitem').filter({ hasText: plate }).getByRole('button', { name: 'Anular' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Motivo de la anulación').fill(reason)
    await dialog.getByRole('button', { name: 'Anular ingreso' }).click()
    await expect(page.getByText(`Ingreso de ${plate} anulado`)).toBeVisible()

    await page.goto('/historial')
    await page.getByLabel('Placa').fill(plate)
    await page.getByLabel('Estado').selectOption('ANULADO')
    await page.getByRole('button', { name: 'Buscar' }).click()
    await expect(page.getByText(`Motivo de anulación: ${reason}`)).toBeVisible()

    // RF-067: filtro de auditoría por entidad.
    await page.goto('/auditoria')
    await page.getByLabel('Entidad').selectOption('ParkingSession')
    await expect(page.getByText('Parqueo anulado').first()).toBeVisible()
  })
})

/**
 * RF-072 / RF-015: con "Imprimir comprobante al registrar el ingreso" activado, tras el ingreso se
 * ofrece el comprobante con consecutivo ING. Solo en desktop: cambia la configuración compartida y el
 * proyecto mobile corre en paralelo; al final se restaura el valor previo.
 */
test.describe('Comprobante de ingreso', () => {
  test.skip(({ isMobile }) => isMobile, 'Modifica la configuración global; se ejecuta una sola vez (desktop).')

  async function setPrintOnEntry(page: Page, enabled: boolean) {
    await page.goto('/configuracion')
    const checkbox = page.getByLabel('Al registrar el ingreso')
    await expect(checkbox).toBeVisible()
    const wasChecked = await checkbox.isChecked()
    if (wasChecked !== enabled) {
      await checkbox.setChecked(enabled)
      await page.getByRole('button', { name: 'Guardar cambios' }).click()
      await expect(page.getByText('Configuración guardada')).toBeVisible()
    }
    return wasChecked
  }

  test('ingreso -> botón Imprimir comprobante -> comprobante con número ING', async ({ page }) => {
    const plate = uniquePlate('ING')
    await login(page)
    const previous = await setPrintOnEntry(page, true)
    try {
      await page.goto('/ingreso')
      await page.getByPlaceholder('ABC123').fill(plate)
      await page.getByRole('button', { name: 'Automóvil' }).click()
      await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
      await page.getByRole('button', { name: 'Imprimir comprobante' }).click()

      const receipt = page.getByRole('article', { name: 'Comprobante de ingreso' })
      await expect(receipt).toBeVisible()
      await expect(receipt.getByText(plate)).toBeVisible()
      await expect(receipt.getByText(/ING-\d{6}/)).toBeVisible()
      await expect(receipt.getByText('Conserve este comprobante para retirar su vehículo.')).toBeVisible()

      // Reimpresión desde Historial.
      await page.goto('/historial')
      await page.getByLabel('Placa').fill(plate)
      await page.getByRole('button', { name: 'Buscar' }).click()
      await expect(page.getByRole('listitem').filter({ hasText: plate }).getByRole('button', { name: 'Comprobante de ingreso' })).toBeVisible()
    } finally {
      await setPrintOnEntry(page, previous)
    }
  })
})
