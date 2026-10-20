import { describe, expect, it } from 'vitest'
import { DEFAULT_RECEIPT_FORMAT, parseReceiptFormat } from './receiptFormat'
import { localIsoDate } from './format'

describe('parseReceiptFormat', () => {
  it('sin formato o con JSON inválido usa los valores por defecto', () => {
    expect(parseReceiptFormat(null)).toEqual(DEFAULT_RECEIPT_FORMAT)
    expect(parseReceiptFormat('{no-json')).toEqual(DEFAULT_RECEIPT_FORMAT)
  })

  it('respeta los campos guardados y completa los ausentes', () => {
    const format = parseReceiptFormat('{"showNit":false,"footer":"Gracias"}')
    expect(format.showNit).toBe(false)
    expect(format.showTariff).toBe(true)
    expect(format.footer).toBe('Gracias')
  })

  it('impresión: por defecto no se ofrece al ingreso y sí a la salida', () => {
    expect(parseReceiptFormat(null)).toMatchObject({ printOnEntry: false, printOnExit: true })
    expect(parseReceiptFormat('{"showNit":true}')).toMatchObject({ printOnEntry: false, printOnExit: true })
  })

  it('impresión: respeta las opciones guardadas por el administrador', () => {
    expect(parseReceiptFormat('{"printOnEntry":true,"printOnExit":false}')).toMatchObject({
      printOnEntry: true,
      printOnExit: false,
    })
  })
})

describe('localIsoDate', () => {
  it('usa la fecha local, no UTC', () => {
    expect(localIsoDate(new Date(2026, 8, 29, 23, 30))).toBe('2026-09-29')
    expect(localIsoDate(new Date(2026, 0, 5, 0, 5))).toBe('2026-01-05')
  })
})
