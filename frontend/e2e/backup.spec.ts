import { readFile } from 'node:fs/promises'
import { expect, test } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * RF-073: el administrador descarga el respaldo completo desde /respaldo. La importación (que
 * reemplaza todo) no se ejecuta aquí porque borraría los datos de las demás pruebas en paralelo;
 * la cubre BackupIntegrationTest en el backend.
 */
test.describe('Respaldo de la base de datos', () => {
  test.skip(({ isMobile }) => isMobile, 'Una sola ejecución (desktop) basta para la descarga.')

  test('descarga un respaldo JSON con todas las tablas', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Usuario').fill(ADMIN_USERNAME)
    await page.getByLabel('Contraseña').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Iniciar sesión' }).click()
    await expect(page).toHaveURL('/')

    await page.goto('/respaldo')
    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: 'Descargar respaldo' }).click()
    const download = await downloadPromise

    expect(download.suggestedFilename()).toMatch(/^parking-respaldo_\d{4}-\d{2}-\d{2}_\d{4}\.json$/)
    const backup = JSON.parse(await readFile(await download.path(), 'utf-8'))
    expect(backup.format).toBe('parking-backup')
    expect(backup.tables.users.length).toBeGreaterThan(0)
    expect(backup.tables.parkingSessions.length).toBe(backup.counts.parkingSessions)

    // Al elegir el archivo se muestra el resumen antes de importar (sin confirmar la importación).
    await page.getByLabel('Archivo de respaldo').setInputFiles(await download.path())
    await expect(page.getByText(/versión de esquema/)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Importar y reemplazar' })).toBeEnabled()
  })
})
