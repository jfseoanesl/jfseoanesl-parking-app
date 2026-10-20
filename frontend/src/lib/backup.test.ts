import { describe, expect, it } from 'vitest'
import { parseBackupFile } from './backup'

describe('parseBackupFile', () => {
  it('lee el resumen de un respaldo válido', () => {
    const text = JSON.stringify({
      format: 'parking-backup',
      formatVersion: 1,
      schemaVersion: '6',
      exportedAt: '2026-10-06T12:00:00Z',
      exportedBy: 'admin',
      sourceDatabase: 'SQLite',
      counts: { users: 2, parkingSessions: 10 },
      tables: { users: [] },
    })
    const summary = parseBackupFile(text)
    expect(summary).toMatchObject({ schemaVersion: '6', exportedBy: 'admin', sourceDatabase: 'SQLite' })
    expect(summary.counts.parkingSessions).toBe(10)
  })

  it('rechaza archivos que no son JSON o no son respaldos de la app', () => {
    expect(() => parseBackupFile('no es json')).toThrow('JSON válido')
    expect(() => parseBackupFile('{"format":"otro","tables":{}}')).toThrow('respaldo de esta aplicación')
    expect(() => parseBackupFile('{"format":"parking-backup"}')).toThrow('respaldo de esta aplicación')
  })
})
