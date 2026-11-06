import { expect, test } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'
import { motorcyclePlate } from './plates'

/** BR-036: una placa de moto registrada como automóvil se bloquea y la pantalla ofrece el cambio. */
test.describe('Formato de placa por tipo de vehículo', () => {
  test.skip(({ isMobile }) => isMobile, 'Una sola ejecución (desktop).')

  test('placa de moto con tipo automovil: aviso, cambio sugerido e ingreso', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
    await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page).toHaveURL('/')

    const plate = motorcyclePlate()
    await page.goto('/ingreso')
    await page.getByRole('button', { name: 'Automóvil' }).click()
    await page.getByPlaceholder('ABC123').fill(plate)

    await expect(page.getByText(`La placa ${plate} tiene formato de motocicleta, no de automóvil`)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Confirmar ingreso' })).toBeDisabled()

    await page.getByRole('button', { name: 'Cambiar a Motocicleta' }).click()
    await expect(page.getByRole('alert').filter({ hasText: 'formato de motocicleta' })).toHaveCount(0)
    await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
    await expect(page.getByText(`Ingreso registrado: ${plate}`)).toBeVisible()
  })

  test('con la validacion desactivada en Configuracion no hay aviso', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
    await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page).toHaveURL('/')

    const toggle = async (checked: boolean) => {
      await page.goto('/configuracion')
      const box = page.getByLabel(/Validar el formato de placa/)
      await box.setChecked(checked)
      await page.getByRole('button', { name: 'Guardar cambios' }).click()
      await expect(page.getByText('Configuración guardada')).toBeVisible()
      await page.reload()
      await expect(page.getByLabel(/Validar el formato de placa/)).toBeChecked({ checked })
    }

    await toggle(false)
    try {
      const plate = motorcyclePlate()
      await page.goto('/ingreso')
      await page.getByRole('button', { name: 'Automóvil' }).click()
      await page.getByPlaceholder('ABC123').fill(plate)
      await expect(page.getByText('tiene formato de motocicleta')).toHaveCount(0)
      await page.getByRole('button', { name: 'Confirmar ingreso' }).click()
      await expect(page.getByText(`Ingreso registrado: ${plate}`)).toBeVisible()
    } finally {
      await toggle(true)
    }
  })
})
