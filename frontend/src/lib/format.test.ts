import { describe, expect, it } from 'vitest'
import { formatCurrency, formatElapsed, formatTime } from './format'

describe('formatCurrency', () => {
  it('formatea como pesos colombianos sin decimales', () => {
    expect(formatCurrency(5000)).toBe('$ 5.000')
  })

  it('formatea cero', () => {
    expect(formatCurrency(0)).toBe('$ 0')
  })
})

describe('formatElapsed', () => {
  it('muestra solo minutos cuando dura menos de una hora', () => {
    expect(formatElapsed(45)).toBe('45m')
  })

  it('muestra horas y minutos cuando supera una hora', () => {
    expect(formatElapsed(125)).toBe('2h 5m')
  })

  it('maneja exactamente una hora', () => {
    expect(formatElapsed(60)).toBe('1h 0m')
  })

  it('maneja cero minutos', () => {
    expect(formatElapsed(0)).toBe('0m')
  })
})

describe('formatTime', () => {
  it('formatea una fecha ISO como hora local de 2 digitos', () => {
    const result = formatTime('2026-09-23T15:30:00Z')
    expect(result).toMatch(/^\d{1,2}:\d{2}\s?(a\.\s?m\.|p\.\s?m\.)$/i)
  })
})
