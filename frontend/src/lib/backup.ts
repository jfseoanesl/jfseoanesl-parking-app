/** RF-073: resumen de un archivo de respaldo, leído en el navegador antes de confirmar la importación. */
export interface BackupSummary {
  exportedAt: string
  exportedBy: string | null
  sourceDatabase: string | null
  schemaVersion: string
  counts: Record<string, number>
  /** Contenido completo, tal cual se envía al servidor. */
  data: unknown
}

/** Etiquetas legibles de las tablas del respaldo (claves de BackupFile.Tables en el backend). */
export const BACKUP_TABLE_LABELS: Record<string, string> = {
  users: 'Usuarios',
  parkingSessions: 'Sesiones de parqueo',
  payments: 'Pagos',
  cashSessions: 'Cajas',
  tariffs: 'Tarifas',
  vehicleTypes: 'Tipos de vehículo',
  paymentMethods: 'Medios de pago',
  auditLogs: 'Registros de auditoría',
}

export function parseBackupFile(text: string): BackupSummary {
  let data: unknown
  try {
    data = JSON.parse(text)
  } catch {
    throw new Error('El archivo no es un JSON válido')
  }
  const backup = data as Record<string, unknown> | null
  if (!backup || backup.format !== 'parking-backup' || typeof backup.tables !== 'object' || backup.tables === null) {
    throw new Error('El archivo no es un respaldo de esta aplicación')
  }
  return {
    exportedAt: String(backup.exportedAt ?? ''),
    exportedBy: typeof backup.exportedBy === 'string' ? backup.exportedBy : null,
    sourceDatabase: typeof backup.sourceDatabase === 'string' ? backup.sourceDatabase : null,
    schemaVersion: String(backup.schemaVersion ?? '?'),
    counts: (backup.counts as Record<string, number> | undefined) ?? {},
    data,
  }
}
