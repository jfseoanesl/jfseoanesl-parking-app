import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { FilePenLine, Receipt, Search } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashOperation, ClosedCashSession } from '../api/types'
import { formatCurrency, formatDateTime, formatTime, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, Skeleton } from '../components/Feedback'
import { TextField } from '../components/Form'
import { Card } from '../components/Layout'
import { Badge, PlateBadge } from '../components/Badges'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'

/** RF-059: cobros registrados en la caja del turno, con acceso al comprobante. */
export function ShiftOperations({ cashSessionId, refreshKey }: { cashSessionId: string; refreshKey: number }) {
  const navigate = useNavigate()
  const [operations, setOperations] = useState<CashOperation[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<CashOperation[]>(`/cash-sessions/${cashSessionId}/payments`)
      .then(setOperations)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las operaciones'))
  }, [cashSessionId, refreshKey])

  return (
    <Card title="Operaciones de mi turno" description="Cobros registrados en esta caja, del más reciente al más antiguo." className="mt-4">
      {error ? (
        <Alert>{error}</Alert>
      ) : operations === null ? (
        <Skeleton className="h-16" />
      ) : operations.length === 0 ? (
        <p className="text-sm text-neutral-500">Aún no hay cobros en este turno.</p>
      ) : (
        <ul className="divide-y divide-neutral-100">
          {operations.map((op) => (
            <li key={op.paymentId} className="flex flex-wrap items-center gap-3 py-3">
              {op.plate && <PlateBadge plate={op.plate} size="sm" />}
              <div className="min-w-0 flex-1 text-sm">
                <p className="font-mono text-neutral-700">{op.operationNumber}</p>
                <p className="text-neutral-500">
                  {formatTime(op.paidAt)} · {op.paymentMethodName ?? '—'}
                  {op.tariffName && <> · {op.tariffName}</>}
                </p>
              </div>
              <span className="font-semibold tabular-nums text-neutral-900">{formatCurrency(op.chargedAmount)}</span>
              <Button
                variant="ghost"
                aria-label={`Ver comprobante ${op.operationNumber}`}
                icon={<Receipt aria-hidden className="size-4" />}
                onClick={() =>
                  navigate(op.kind === 'SUBSCRIPTION' ? `/comprobantes-mensualidad/${op.paymentId}` : `/comprobantes/${op.paymentId}`)
                }
              >
                Comprobante
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return localIsoDate(d)
}

function differenceTone(diff: number): 'success' | 'danger' | 'warning' {
  return diff === 0 ? 'success' : diff < 0 ? 'danger' : 'warning'
}

/** RF-057 / BR-023: el Administrador consulta cajas cerradas y corrige el efectivo reportado. */
export function ClosedCashSessionsAdmin() {
  const toast = useToast()
  const [from, setFrom] = useState(isoDaysAgo(6))
  const [to, setTo] = useState(isoDaysAgo(0))
  const [sessions, setSessions] = useState<ClosedCashSession[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<ClosedCashSession | null>(null)
  const [reportedCash, setReportedCash] = useState('')
  const [reason, setReason] = useState('')
  const [dialogError, setDialogError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function load() {
    setError(null)
    api
      .get<ClosedCashSession[]>(`/cash-sessions?from=${from}&to=${to}`)
      .then(setSessions)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cajas cerradas'))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [])

  function handleSearch(event: FormEvent) {
    event.preventDefault()
    load()
  }

  async function confirmAdjustment() {
    if (!editing) return
    if (reportedCash === '' || Number(reportedCash) < 0 || !reason.trim()) {
      setDialogError('Indica el nuevo efectivo reportado y el motivo de la corrección')
      return
    }
    setSubmitting(true)
    setDialogError(null)
    try {
      await api.post(`/cash-sessions/${editing.id}/adjustments`, { reportedCash: Number(reportedCash), reason: reason.trim() })
      toast('Cierre de caja corregido')
      setEditing(null)
      load()
    } catch (err) {
      setDialogError(err instanceof ApiError ? err.message : 'No se pudo corregir el cierre')
    } finally {
      setSubmitting(false)
    }
  }

  const newDiff = editing && reportedCash !== '' ? Number(reportedCash) - (editing.expectedCash ?? 0) : null

  return (
    <Card title="Cajas cerradas" description="Consulta los cierres y corrige el efectivo reportado (queda auditado)." className="mt-6">
      <form onSubmit={handleSearch} className="mb-4 grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
        <TextField label="Cerradas desde" type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
        <TextField label="Hasta" type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} />
        <Button type="submit" variant="secondary" icon={<Search aria-hidden className="size-5" />} disabled={!from || !to || from > to}>
          Buscar
        </Button>
      </form>

      {error ? (
        <Alert>{error}</Alert>
      ) : sessions === null ? (
        <Skeleton className="h-16" />
      ) : sessions.length === 0 ? (
        <p className="text-sm text-neutral-500">No hay cajas cerradas en ese rango.</p>
      ) : (
        <ul className="divide-y divide-neutral-100">
          {sessions.map((s) => {
            const diff = s.difference ?? 0
            return (
              <li key={s.id} className="flex flex-wrap items-center gap-3 py-3">
                <div className="min-w-0 flex-1 text-sm">
                  <p className="font-medium text-neutral-900">{s.employeeName ?? 'Empleado'}</p>
                  <p className="text-neutral-500 tabular-nums">
                    {formatDateTime(s.openedAt)} – {formatDateTime(s.closedAt)} · {s.vehiclesServed ?? 0} vehículos
                  </p>
                </div>
                <div className="text-right text-sm tabular-nums">
                  <p className="text-neutral-600">
                    Esperado {formatCurrency(s.expectedCash ?? 0)} · Reportado {formatCurrency(s.reportedCash ?? 0)}
                  </p>
                  <Badge tone={differenceTone(diff)}>Diferencia {formatCurrency(diff)}</Badge>
                </div>
                <Button
                  variant="ghost"
                  icon={<FilePenLine aria-hidden className="size-4" />}
                  onClick={() => {
                    setReportedCash(s.reportedCash != null ? String(s.reportedCash) : '')
                    setReason('')
                    setDialogError(null)
                    setEditing(s)
                  }}
                >
                  Corregir
                </Button>
              </li>
            )
          })}
        </ul>
      )}

      <ConfirmDialog
        open={editing !== null}
        title={`Corregir cierre de ${editing?.employeeName ?? 'caja'}`}
        confirmLabel="Guardar corrección"
        loading={submitting}
        onConfirm={confirmAdjustment}
        onCancel={() => setEditing(null)}
      >
        <p>Efectivo esperado: {formatCurrency(editing?.expectedCash ?? 0)}</p>
        <div className="mt-4 space-y-4">
          <TextField
            label="Nuevo efectivo reportado"
            type="number"
            inputMode="numeric"
            min="0"
            adornment="$"
            value={reportedCash}
            onChange={(e) => setReportedCash(e.target.value)}
            hint={newDiff != null ? `Nueva diferencia: ${formatCurrency(newDiff)}` : undefined}
            required
          />
          <div>
            <label className="label" htmlFor="adjust-reason">
              Motivo de la corrección
            </label>
            <textarea
              id="adjust-reason"
              className="input min-h-20"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={255}
              required
            />
          </div>
          {dialogError && <p className="text-danger-700">{dialogError}</p>}
        </div>
      </ConfirmDialog>
    </Card>
  )
}
