import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Ban, CirclePlus, Clock, LogOut, Search, SearchX, SquareParking } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { ParkingSession } from '../api/types'
import { formatElapsed, formatTime } from '../lib/format'
import { Button } from '../components/Button'
import { EmptyState, Skeleton } from '../components/Feedback'
import { PageHeader } from '../components/Layout'
import { PlateBadge, StatusBadge, VehicleIcon } from '../components/Badges'
import { SelectField } from '../components/Form'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'
import { useAuth } from '../auth/AuthContext'

type SortKey = 'recent' | 'oldest' | 'longest' | 'shortest'

const SORTERS: Record<SortKey, (a: ParkingSession, b: ParkingSession) => number> = {
  recent: (a, b) => b.entryDateTime.localeCompare(a.entryDateTime),
  oldest: (a, b) => a.entryDateTime.localeCompare(b.entryDateTime),
  longest: (a, b) => b.elapsedMinutes - a.elapsedMinutes,
  shortest: (a, b) => a.elapsedMinutes - b.elapsedMinutes,
}

/** RF-030 a RF-033 (filtro por tipo y orden, RF-032) y anulación administrativa (RF-068). Wireframe: 09-ux-specification.md §5.4 (lista en movil, tabla en desktop). */
export default function ActiveSessionsPage() {
  const navigate = useNavigate()
  const [plateFilter, setPlateFilter] = useState('')
  const [sessions, setSessions] = useState<ParkingSession[]>([])
  const [loading, setLoading] = useState(true)
  const [typeFilter, setTypeFilter] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('recent')
  const [cancelling, setCancelling] = useState<ParkingSession | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelError, setCancelError] = useState<string | null>(null)
  const [cancelSubmitting, setCancelSubmitting] = useState(false)
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const vehicleTypes = useMemo(() => {
    const map = new Map<string, string>()
    sessions.forEach((s) => map.set(s.vehicleTypeCode ?? s.vehicleTypeName ?? '', s.vehicleTypeName ?? 'Otro'))
    return [...map.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [sessions])

  const visible = useMemo(
    () => sessions.filter((s) => !typeFilter || (s.vehicleTypeCode ?? s.vehicleTypeName ?? '') === typeFilter).sort(SORTERS[sortKey]),
    [sessions, typeFilter, sortKey],
  )

  async function confirmCancel() {
    if (!cancelling) return
    if (!cancelReason.trim()) {
      setCancelError('El motivo es obligatorio')
      return
    }
    setCancelSubmitting(true)
    setCancelError(null)
    try {
      await api.post(`/parking-sessions/${cancelling.id}/cancel`, { reason: cancelReason.trim() })
      toast(`Ingreso de ${cancelling.plate} anulado`)
      setCancelling(null)
      load(plateFilter)
    } catch (err) {
      setCancelError(err instanceof ApiError ? err.message : 'No se pudo anular el ingreso')
    } finally {
      setCancelSubmitting(false)
    }
  }

  function load(filter: string) {
    const query = filter ? `?plate=${encodeURIComponent(filter)}` : ''
    api
      .get<ParkingSession[]>(`/parking-sessions/active${query}`)
      .then(setSessions)
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load(plateFilter)
    // Refresco periodico para mantener el tiempo transcurrido razonablemente al dia.
    const interval = setInterval(() => load(plateFilter), 30000)
    return () => clearInterval(interval)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [plateFilter])

  return (
    <div>
      <PageHeader
        title="Vehículos parqueados"
        description={loading ? 'Cargando…' : `${visible.length} ${visible.length === 1 ? 'vehículo' : 'vehículos'} ${plateFilter || typeFilter ? 'encontrados' : 'en el parqueadero'}`}
        actions={
          <Link
            to="/ingreso"
            className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-primary-600 px-4 text-sm font-medium text-white shadow-sm transition-colors hover:bg-primary-700"
          >
            <CirclePlus aria-hidden className="size-5" />
            Nuevo ingreso
          </Link>
        }
      />

      <div className="mb-4 grid gap-3 sm:grid-cols-[1fr_12rem_14rem] sm:items-end">
      <div className="relative">
        <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-neutral-400" />
        <input
          type="search"
          aria-label="Buscar placa"
          className="input min-h-12 pl-11 font-mono text-lg tracking-wider uppercase placeholder:font-sans placeholder:tracking-normal placeholder:normal-case"
          placeholder="Buscar placa…"
          value={plateFilter}
          onChange={(e) => setPlateFilter(e.target.value.toUpperCase().replace(/\s+/g, ''))}
          autoComplete="off"
          spellCheck={false}
        />
      </div>
        <SelectField label="Tipo de vehículo" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
          <option value="">Todos</option>
          {vehicleTypes.map(([code, name]) => (
            <option key={code} value={code}>
              {name}
            </option>
          ))}
        </SelectField>
        <SelectField label="Ordenar por" value={sortKey} onChange={(e) => setSortKey(e.target.value as SortKey)}>
          <option value="recent">Ingreso más reciente</option>
          <option value="oldest">Ingreso más antiguo</option>
          <option value="longest">Mayor tiempo</option>
          <option value="shortest">Menor tiempo</option>
        </SelectField>
      </div>

      {loading ? (
        <div className="flex flex-col gap-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-28 md:h-16" />
          ))}
        </div>
      ) : visible.length === 0 ? (
        plateFilter || typeFilter ? (
          <EmptyState
            icon={<SearchX aria-hidden className="size-6" />}
            title={plateFilter ? `No hay vehículos con la placa "${plateFilter}"` : 'No hay vehículos de este tipo'}
            description="Revisa los filtros o limpia la búsqueda."
          />
        ) : (
          <EmptyState
            icon={<SquareParking aria-hidden className="size-6" />}
            title="No hay vehículos parqueados"
            description="Cuando registres un ingreso aparecerá aquí."
          />
        )
      ) : (
        <div className="md:overflow-hidden md:rounded-xl md:border md:border-neutral-200 md:bg-white md:shadow-sm">
          <div
            aria-hidden
            className="hidden grid-cols-[minmax(15rem,1.4fr)_1fr_1fr_1fr_auto] gap-4 border-b border-neutral-200 bg-neutral-50 px-5 py-3 md:grid"
          >
            {['Placa', 'Tipo', 'Ingreso', 'Empleado', 'Acciones'].map((h) => (
              <span key={h} className="section-title last:text-right">
                {h}
              </span>
            ))}
          </div>
          <ul className="flex flex-col gap-2 md:gap-0 md:divide-y md:divide-neutral-100">
            {visible.map((session) => (
              <li
                key={session.id}
                className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm md:grid md:grid-cols-[minmax(15rem,1.4fr)_1fr_1fr_1fr_auto] md:items-center md:gap-4 md:rounded-none md:border-0 md:px-5 md:py-3 md:shadow-none md:transition-colors md:hover:bg-neutral-50"
              >
                <div className="flex flex-wrap items-center justify-between gap-2 md:justify-start">
                  <PlateBadge plate={session.plate} />
                  <StatusBadge status={session.status} />
                </div>
                <p className="mt-3 flex items-center gap-2 text-sm text-neutral-700 md:mt-0">
                  <VehicleIcon code={session.vehicleTypeCode} className="size-5 text-neutral-500" />
                  {session.vehicleTypeName}
                </p>
                <p className="mt-1 flex items-center gap-2 text-sm text-neutral-700 md:mt-0">
                  <Clock aria-hidden className="size-5 text-neutral-500" />
                  <span className="tabular-nums">
                    {formatTime(session.entryDateTime)}
                    <span className="text-neutral-500"> · {formatElapsed(session.elapsedMinutes)}</span>
                  </span>
                </p>
                <p className="mt-1 truncate text-sm text-neutral-600 md:mt-0">
                  <span className="md:hidden">Empleado: </span>
                  {session.entryEmployeeName ?? '—'}
                </p>
                <div className="mt-4 flex flex-col gap-2 sm:flex-row md:mt-0 md:justify-end">
                  {isAdmin && (
                    <Button
                      variant="ghost"
                      onClick={() => {
                        setCancelReason('')
                        setCancelError(null)
                        setCancelling(session)
                      }}
                      icon={<Ban aria-hidden className="size-5" />}
                    >
                      Anular
                    </Button>
                  )}
                  <Button onClick={() => navigate(`/cobros/${session.id}`)} icon={<LogOut aria-hidden className="size-5" />}>
                    Registrar salida
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      <ConfirmDialog
        open={cancelling !== null}
        title={`Anular ingreso ${cancelling?.plate ?? ''}`}
        confirmLabel="Anular ingreso"
        tone="danger"
        loading={cancelSubmitting}
        onConfirm={confirmCancel}
        onCancel={() => setCancelling(null)}
      >
        <p>La sesión quedará como ANULADA, sin cobro, y se registrará en la auditoría.</p>
        <label className="label mt-4" htmlFor="cancel-reason">
          Motivo de la anulación
        </label>
        <textarea
          id="cancel-reason"
          className="input min-h-20"
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          maxLength={255}
          required
        />
        {cancelError && <p className="mt-2 text-danger-700">{cancelError}</p>}
      </ConfirmDialog>
    </div>
  )
}
