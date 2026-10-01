import { useEffect, useState } from 'react'
import { ChevronRight, ShieldCheck } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { AdminUser, AuditLogEntry } from '../api/types'
import { formatDateTime } from '../lib/format'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { SelectField, TextField } from '../components/Form'
import { PageHeader } from '../components/Layout'
import { Badge } from '../components/Badges'

/** Nombres legibles de las acciones auditadas; un codigo desconocido se muestra tal cual. */
const ACTION_LABELS: Record<string, string> = {
  TARIFF_CREATED: 'Tarifa creada',
  TARIFF_UPDATED: 'Tarifa modificada',
  TARIFF_ACTIVATED: 'Tarifa activada',
  TARIFF_DEACTIVATED: 'Tarifa desactivada',
  USER_CREATED: 'Usuario creado',
  USER_ACTIVATED: 'Usuario activado',
  USER_DEACTIVATED: 'Usuario desactivado',
  USER_PASSWORD_CHANGED: 'Contraseña cambiada',
  CASH_SESSION_CLOSED: 'Caja cerrada',
  CASH_SESSION_ADJUSTED: 'Cierre de caja corregido',
  PARKING_SESSION_CANCELLED: 'Parqueo anulado',
  TARIFF_OVERRIDDEN: 'Tarifa cambiada al cobrar',
  CONFIGURATION_UPDATED: 'Configuración modificada',
}

const ENTITY_LABELS: Record<string, string> = {
  Tariff: 'Tarifas',
  User: 'Usuarios',
  CashSession: 'Caja',
  ParkingSession: 'Sesiones de parqueo',
  Configuration: 'Configuración',
}

/** Acciones sensibles resaltadas (anulaciones, correcciones de caja). */
function actionTone(action: string): 'danger' | 'warning' | 'neutral' {
  if (action.endsWith('_CANCELLED') || action.endsWith('_DEACTIVATED')) return 'danger'
  if (action.endsWith('_ADJUSTED') || action.endsWith('_PASSWORD_CHANGED') || action === 'TARIFF_OVERRIDDEN') return 'warning'
  return 'neutral'
}

/** Muestra un JSON guardado en auditoria de forma legible; si no es JSON, tal cual. */
function prettyValue(value: string | null): string {
  if (!value) return '—'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

/** UC-012 (RF-067). No listada entre las 11 pantallas mínimas pero exigida por prompt.md §15. */
export default function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([])
  const [entityFilter, setEntityFilter] = useState('')
  const [userFilter, setUserFilter] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const rangeInvalid = from !== '' && to !== '' && from > to

  useEffect(() => {
    api.get<AdminUser[]>('/users').then(setUsers).catch(() => setUsers([]))
  }, [])

  // RF-067: filtros por entidad, usuario y rango de fechas.
  useEffect(() => {
    if (rangeInvalid) return
    const params = new URLSearchParams()
    if (entityFilter) params.set('entity', entityFilter)
    if (userFilter) params.set('userId', userFilter)
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    const query = params.size > 0 ? `?${params}` : ''
    setLoading(true)
    setError(null)
    api
      .get<AuditLogEntry[]>(`/audit-logs${query}`)
      .then(setLogs)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar la auditoría'))
      .finally(() => setLoading(false))
  }, [entityFilter, userFilter, from, to, rangeInvalid])

  return (
    <div>
      <PageHeader title="Auditoría" description="Registro de cambios administrativos y operaciones sensibles." />

      <div className="card mb-4 grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
        <SelectField label="Entidad" value={entityFilter} onChange={(e) => setEntityFilter(e.target.value)}>
          <option value="">Todas las entidades</option>
          {Object.entries(ENTITY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
        <SelectField label="Usuario" value={userFilter} onChange={(e) => setUserFilter(e.target.value)}>
          <option value="">Todos los usuarios</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </SelectField>
        <TextField label="Desde" type="date" value={from} max={to || undefined} onChange={(e) => setFrom(e.target.value)} />
        <TextField label="Hasta" type="date" value={to} min={from || undefined} onChange={(e) => setTo(e.target.value)} />
      </div>
      {rangeInvalid && <Alert tone="warning" className="mb-4">La fecha inicial debe ser anterior o igual a la final.</Alert>}

      {error && <Alert className="mb-4">{error}</Alert>}

      {loading ? (
        <div className="flex flex-col gap-2">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      ) : logs.length === 0 ? (
        <EmptyState icon={<ShieldCheck aria-hidden className="size-6" />} title="No hay registros de auditoría" />
      ) : (
        <ul className="card divide-y divide-neutral-100">
          {logs.map((log) => (
            <li key={log.id}>
              <details className="group">
                <summary className="flex min-h-14 list-none flex-wrap items-center gap-x-3 gap-y-1 px-5 py-3 transition-colors hover:bg-neutral-50 [&::-webkit-details-marker]:hidden">
                  <ChevronRight aria-hidden className="size-4 shrink-0 text-neutral-400 transition-transform duration-150 group-open:rotate-90" />
                  <Badge tone={actionTone(log.action)}>{ACTION_LABELS[log.action] ?? log.action}</Badge>
                  <span className="text-sm text-neutral-700">{log.userFullName}</span>
                  <span className="ml-auto text-sm tabular-nums text-neutral-500">{formatDateTime(log.occurredAt)}</span>
                </summary>
                <div className="space-y-3 bg-neutral-50 px-5 py-4 text-sm">
                  <p className="text-neutral-600">
                    {ENTITY_LABELS[log.entityName] ?? log.entityName} ·{' '}
                    <span className="font-mono text-xs text-neutral-500">{log.entityId}</span>
                  </p>
                  <div className="grid gap-3 md:grid-cols-2">
                    <ValueBlock title="Valor anterior" value={log.oldValue} />
                    <ValueBlock title="Valor nuevo" value={log.newValue} />
                  </div>
                </div>
              </details>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function ValueBlock({ title, value }: { title: string; value: string | null }) {
  return (
    <div className="min-w-0">
      <p className="section-title mb-1">{title}</p>
      <pre className="overflow-x-auto rounded-lg border border-neutral-200 bg-white p-3 font-mono text-xs whitespace-pre-wrap break-all text-neutral-700">
        {prettyValue(value)}
      </pre>
    </div>
  )
}
