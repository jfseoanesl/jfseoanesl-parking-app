import { describe, expect, it } from 'vitest'
import { detectPlateType, plateFormatIssue } from './plateFormat'

describe('plateFormat (BR-036)', () => {
  it('detecta el tipo por el formato', () => {
    expect(detectPlateType('ABC123')).toBe('CAR')
    expect(detectPlateType('ABC12D')).toBe('MOTORCYCLE')
    expect(detectPlateType('ABC12')).toBe('MOTORCYCLE')
    expect(detectPlateType('AB1234')).toBeNull()
  })

  it('acepta la placa del formato de su tipo', () => {
    expect(plateFormatIssue('ABC123', 'CAR')).toBeNull()
    expect(plateFormatIssue('ABC12D', 'MOTORCYCLE')).toBeNull()
    expect(plateFormatIssue('ABC12', 'MOTORCYCLE')).toBeNull()
  })

  it('sugiere el tipo correcto cuando la placa es de otro tipo', () => {
    expect(plateFormatIssue('ABC12D', 'CAR')).toEqual({
      message: 'La placa ABC12D tiene formato de motocicleta, no de automóvil',
      suggested: 'MOTORCYCLE',
    })
    expect(plateFormatIssue('ABC123', 'MOTORCYCLE')?.suggested).toBe('CAR')
  })

  it('indica el formato esperado si no se reconoce, y no valida otros tipos', () => {
    expect(plateFormatIssue('AB1234', 'CAR')).toEqual({
      message: 'La placa AB1234 no tiene el formato de automóvil (ABC123)',
      suggested: null,
    })
    expect(plateFormatIssue('BICI01', 'BICYCLE')).toBeNull()
  })
})
