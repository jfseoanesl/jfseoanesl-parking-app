import { expect, test, type Page } from '@playwright/test'
import { ADMIN_PASSWORD, ADMIN_USERNAME } from './credentials'

/**
 * UC-014: otro ingreso en la caja propia -> gasto de caja (administrador) -> el efectivo esperado
 * lo refleja -> comprobante -> pestaña "Ingresos y gastos" de Reportes. Solo desktop.
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

function parseMoney(text: string): number {
  return Number(text.replace(/[^\d-]/g, ''))
}

test.describe('Ingresos y gastos de caja', () => {
  test.skip(({ isMobile }) => isMobile, 'Una sola ejecución (desktop).')

  test('otro ingreso y gasto afectan el efectivo esperado y salen en Reportes', async ({ page }) => {
    const suffix = Date.now().toString().slice(-5)
    const incomeDescription = `Lavado E2E ${suffix}`

    await login(page)
    await ensureCashSessionOpen(page)

    const expected = page.getByText('Efectivo esperado en caja').locator('..')
    const before = parseMoney(await expected.innerText())

    // Otro ingreso en efectivo en la caja propia.
    await page.getByRole('button', { name: 'Registrar otro ingreso' }).click()
    const incomeDialog = page.getByRole('dialog', { name: 'Registrar otro ingreso' })
    await incomeDialog.getByLabel('Descripción').fill(incomeDescription)
    await incomeDialog.getByLabel('Valor').fill('6000')
    await incomeDialog.getByRole('button', { name: 'Registrar ingreso' }).click()
    const incomeDone = page.getByRole('dialog', { name: 'Ingreso registrado' })
    await expect(incomeDone).toBeVisible()
    await incomeDone.getByRole('button', { name: 'Listo', exact: true }).click()
    await expect.poll(async () => parseMoney(await expected.innerText())).toBe(before + 6000)

    // Gasto de caja desde la sección del administrador (su propia caja abierta).
    const expenses = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Gastos de caja', exact: true }) })
    const me = (await (await page.request.get('/api/auth/me')).json()) as { fullName: string }
    await expenses.getByRole('listitem').filter({ hasText: me.fullName }).getByRole('button', { name: 'Registrar gasto' }).click()
    const expenseDialog = page.getByRole('dialog', { name: 'Registrar gasto de caja' })
    await expenseDialog.getByLabel('Descripción').fill(`Papelería E2E ${suffix}`)
    await expenseDialog.getByLabel('Valor').fill('2000')
    await expenseDialog.getByRole('button', { name: 'Registrar gasto' }).click()
    const expenseDone = page.getByRole('dialog', { name: 'Gasto registrado' })
    await expect(expenseDone).toBeVisible()

    // Comprobante del gasto.
    await expenseDone.getByRole('button', { name: 'Ver comprobante', exact: true }).click()
    const receipt = page.getByRole('article', { name: 'Comprobante de gasto' })
    await expect(receipt).toBeVisible()
    await expect(receipt.getByText(/OP-\d{6}/)).toBeVisible()

    await page.goto('/caja')
    await expect.poll(async () => parseMoney(await expected.innerText())).toBe(before + 6000 - 2000)
    await expect(page.getByText(incomeDescription)).toBeVisible()

    // Reportes -> Ingresos y gastos.
    await page.goto('/reportes')
    await page.getByRole('button', { name: 'Ingresos y gastos' }).click()
    await expect(page.getByRole('heading', { name: 'Ingresos y gastos de caja' })).toBeVisible()
    await expect(page.getByText(incomeDescription)).toBeVisible()
  })

  test('la opcion del menu lateral permite registrar y ver otros ingresos', async ({ page }) => {
    const description = `Menu E2E ${Date.now().toString().slice(-5)}`
    await login(page)
    await ensureCashSessionOpen(page)

    await page.getByRole('navigation').getByRole('link', { name: 'Ingresos y gastos' }).click()
    await expect(page).toHaveURL('/ingresos-gastos')
    await expect(page.getByRole('heading', { name: 'Ingresos y gastos', exact: true })).toBeVisible()

    await page.getByRole('button', { name: 'Registrar otro ingreso' }).click()
    const dialog = page.getByRole('dialog', { name: 'Registrar otro ingreso' })
    await dialog.getByLabel('Descripción').fill(description)
    await dialog.getByLabel('Valor').fill('3500')
    await dialog.getByRole('button', { name: 'Registrar ingreso' }).click()
    const done = page.getByRole('dialog', { name: 'Ingreso registrado' })
    await expect(done).toBeVisible()
    await done.getByRole('button', { name: 'Listo', exact: true }).click()

    const mine = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Movimientos de mi caja', exact: true }) })
    await expect(mine.getByText(description)).toBeVisible()
    const today = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Movimientos de hoy', exact: true }) })
    await expect(today.getByText(description)).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Gastos de caja', exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Conceptos de caja', exact: true })).toBeVisible()
  })
})
