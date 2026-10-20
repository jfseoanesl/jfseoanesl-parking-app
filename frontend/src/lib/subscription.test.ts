import { describe, expect, it } from 'vitest'
import { matchesSubscription, SUBSCRIPTION_STATUS } from './subscription'

const sub = { customerName: 'Ana Gómez', documentId: '1020', vehicles: [{ plate: 'ABC123' }, { plate: 'XYZ98F' }] }

describe('matchesSubscription', () => {
  it('busca por cliente, documento o placa', () => {
    expect(matchesSubscription(sub, '')).toBe(true)
    expect(matchesSubscription(sub, 'ana')).toBe(true)
    expect(matchesSubscription(sub, '1020')).toBe(true)
    expect(matchesSubscription(sub, 'abc 123')).toBe(true)
    expect(matchesSubscription(sub, 'zzz')).toBe(false)
  })
})

describe('SUBSCRIPTION_STATUS', () => {
  it('cubre todos los estados con etiqueta y tono', () => {
    expect(SUBSCRIPTION_STATUS.AL_DIA.tone).toBe('success')
    expect(SUBSCRIPTION_STATUS.VENCIDA.label).toBe('Vencida')
    expect(Object.keys(SUBSCRIPTION_STATUS)).toHaveLength(5)
  })
})
