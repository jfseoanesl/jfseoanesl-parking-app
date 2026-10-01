import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Clock, History, Receipt, Search, User } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { AdminUser, HistoryPage as HistoryResult, ParkingStatus } from '../api/types'
import { formatCurrency, formatDateTime, formatElapsed, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { SelectField, TextField } from '../components/Form'
import { PageHeader } from '../components/Layout'
import { PlateBadge, StatusBadge, VehicleIcon } from '../components/Badges'

function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return localIsoDate(d)
}

interface Filters {
  from: string
  to: string
  plate: string
  status: '' | ParkingStatus
  employeeId: string
}

/** RF-058: historial de sesiones con filtros por fecha, placa, estado y empleado (Administrador). */
export default function HistoryPage() {
  const navigate = useNavigate()
  const [filters, setFilters] = useState<Filters>({ from: isoDaysAgo(6), to: isoDaysAgo(0), plate: '', status: '', employeeId: '' })
  const [users, setUsers] = useState<AdminUser[]>([])
  const [result, setResult] = useState<HistoryResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  function search(f: Filters) {
    setLoading(true)
    setError(null)
    const params = new URLSearchParams({ from: f.from, to: f.to })
    if (f.plate.trim()) params.set('plate', f.plate.trim())
    if (f.status) params.set('status', f.status)
    if (f.employeeId) params.set('employeeId', f.employeeId)
    api
      .get<HistoryResult>(`/parking-sessions?${params}`)
      .then(setResult)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo consultar el historial'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    api.get<AdminUser[]>('/users').then(setUsers).catch(() => setUsers([]))
    search(filters)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    search(filters)
  }

  const set = <K extends keyof Filters>(key: K, value: Filters[K]) => setFilters((f) => ({ ...f, [key]: value }))
  const invalidRange = !filters.from || !filters.to || filters.from > filters.to

  return (
    <div>
      <PageHeader
        title="Historial"
        description={
          result && !loading
            ? `${result.items.length} ${result.items.length === 1 ? 'sesión' : 'sesiones'} por fecha de ingreso`
            : 'Sesiones de parqueo activas, finalizadas y anuladas'
        }
      />

      <form onSubmit={handleSubmit} className="card mb-6 grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-6 lg:items-end">
        <TextField label="Desde" type="date" value={filters.from} max={filters.to} onChange={(e) => set('from', e.target.value)} />
        <TextField label="Hasta" type="date" value={filters.to} min={filters.from} onChange={(e) => set('to', e.target.value)} />
        <TextField
          label="Placa"
          value={filters.plate}
          onChange={(e) => set('plate', e.target.value.toUpperCase().replace(/\s+/g, ''))}
          placeholder="Ej. ABC"
          autoComplete="off"
        />
        <SelectField label="Estado" value={filters.status} onChange={(e) => set('status', e.target.value as Filters['status'])}>
          <option value="">Todos</option>
          <option value="EN_PARQUEADERO">En parqueadero</option>
          <option value="FINALIZADO">Finalizado</option>
          <option value="ANULADO">Anulado</option>
        </SelectField>
        <SelectField label="Empleado" value={filters.employeeId} onChange={(e) => set('employeeId', e.target.value)}>
          <option value="">Todos</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </SelectField>
        <Button type="submit" icon={<Search aria-hidden className="size-5" />} disabled={invalidRange} loading={loading}>
          Buscar
        </Button>
      </form>

      {error && <Alert className="mb-4">{error}</Alert>}
      {result?.truncated && (
        <Alert tone="warning" className="mb-4">
          Hay más resultados de los que se muestran. Acota el rango de fechas o agrega filtros.
        </Alert>
      )}

      {loading ? (
        <div className="flex flex-col gap-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : !result || result.items.length === 0 ? (
        <EmptyState
          icon={<History aria-hidden className="size-6" />}
          title="Sin sesiones para estos filtros"
          description="Prueba con otro rango de fechas o quita algún filtro."
        />
      ) : (
        <ul className="flex flex-col gap-2">
          {result.items.map((item) => (
            <li key={item.id} className="card p-4">
              <div className="flex flex-wrap items-center gap-3">
                <PlateBadge plate={item.plate} size="sm" />
                <StatusBadge status={item.status} />
                <span className="flex items-center gap-1.5 text-sm text-neutral-600">
                  <VehicleIcon code={item.vehicleTypeCode} className="size-4" />
                  {item.vehicleTypeName}
                </span>
                {item.chargedAmount != null && (
                  <span className="ml-auto text-lg font-semibold text-neutral-900">{formatCurrency(item.chargedAmount)}</span>
                )}
              </div>
              <div className="mt-3 grid gap-x-6 gap-y-1 text-sm text-neutral-700 sm:grid-cols-2">
                <p className="flex items-center gap-2">
                  <Clock aria-hidden className="size-4 text-neutral-500" />
                  <span className="tabular-nums">
                    {formatDateTime(item.entryDateTime)}
                    {item.exitDateTime && (
                      <>
                        {' '}
                        <ArrowRight aria-hidden className="inline size-3.5" /> {formatDateTime(item.exitDateTime)}
                      </>
                    )}
                    <span className="text-neutral-500"> · {formatElapsed(item.elapsedMinutes)}</span>
                  </span>
                </p>
                <p className="flex items-center gap-2">
                  <User aria-hidden className="size-4 text-neutral-500" />
                  Ingresó: {item.entryEmployeeName ?? '—'}
                  {item.exitEmployeeName && <> · Cobró: {item.exitEmployeeName}</>}
                </p>
                {item.appliedTariffName && <p className="text-neutral-600">Tarifa: {item.appliedTariffName}</p>}
                {item.operationNumber && <p className="font-mono text-neutral-600">{item.operationNumber}</p>}
                {item.notes && <p className="text-neutral-600 sm:col-span-2">Observaciones: {item.notes}</p>}
                {item.cancelReason && <p className="text-danger-700 sm:col-span-2">Motivo de anulación: {item.cancelReason}</p>}
              </div>
              {item.paymentId && (
                <div className="mt-3 border-t border-neutral-100 pt-3">
                  <Button
                    variant="secondary"
                    icon={<Receipt aria-hidden className="size-4" />}
                    onClick={() => navigate(`/comprobantes/${item.paymentId}`)}
                  >
                    Ver comprobante
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
