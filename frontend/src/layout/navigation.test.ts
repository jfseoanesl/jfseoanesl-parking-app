import { describe, expect, it } from 'vitest'
import { mobileNavForRole, navForRole } from './navigation'

describe('navForRole', () => {
  it('el empleado ve solo su menu operativo', () => {
    const items = navForRole('EMPLOYEE')
    expect(items.map((i) => i.path)).toEqual(['/', '/ingreso', '/parqueados', '/parqueados', '/caja', '/mensualidades'])
    expect(items.every((i) => i.implemented)).toBe(true)
  })

  it('el administrador ve el menu completo, con Historial implementado', () => {
    const items = navForRole('ADMIN')
    expect(items.find((i) => i.path === '/historial')?.implemented).toBe(true)
  })
})

describe('mobileNavForRole', () => {
  it('el empleado ve 4 primarios y el resto en "Mas"', () => {
    const { primary, overflow } = mobileNavForRole('EMPLOYEE')
    expect(primary).toHaveLength(4)
    expect(overflow.map((i) => i.path)).toContain('/mensualidades')
  })

  it('el administrador prioriza items implementados en los 4 espacios primarios', () => {
    const { primary, overflow } = mobileNavForRole('ADMIN')
    expect(primary).toHaveLength(4)
    expect(primary.every((i) => i.implemented)).toBe(true)
    expect(overflow.length).toBeGreaterThan(0)
  })
})
