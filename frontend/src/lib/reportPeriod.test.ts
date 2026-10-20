import { describe, expect, it } from 'vitest'
import { rangeForPreset } from './reportPeriod'

const TODAY = new Date(2026, 9, 20, 22, 30) // 20-oct-2026, 10:30 p. m. local

describe('rangeForPreset', () => {
  it('diario, semanal y mensual en fecha local', () => {
    expect(rangeForPreset('daily', '', '', TODAY)).toEqual({ from: '2026-10-20', to: '2026-10-20' })
    expect(rangeForPreset('weekly', '', '', TODAY)).toEqual({ from: '2026-10-14', to: '2026-10-20' })
    expect(rangeForPreset('monthly', '', '', TODAY)).toEqual({ from: '2026-09-21', to: '2026-10-20' })
  })

  it('rango personalizado tal cual', () => {
    expect(rangeForPreset('custom', '2026-01-01', '2026-01-31', TODAY)).toEqual({ from: '2026-01-01', to: '2026-01-31' })
  })
})
