import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Ban, FilePenLine, MinusCircle, Receipt, Search } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashOperation, ClosedCashSession, OpenCashSession } from '../api/types'
import { formatCurrency, formatDateTime, formatTime, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, Skeleton } from '../components/Feedback'
import { TextField } from '../components/Form'
import { Card } from '../components/Layout'
import { Badge, PlateBadge } from '../components/Badges'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'
import { useAuth } from '../auth/AuthContext'
import { CashTransactionDialog } from '../components/CashTransactionDialog'

/** Comprobante de cada tipo de operación de caja. */
function receiptPath(op: CashOperation): string {
  if (op.kind === 'SUBSCRIPTION') return `/comprobantes-mensualidad/${op.paymentId}`
  if (op.kind === 'INCOME' || op.kind === 'EXPENSE') return `/comprobantes-movimiento/${op.paymentId}`
  return `/comprobantes/${op.paymentId}`
}

/**
 * RF-059: cobros, otros ingresos y gastos registrados en la caja del turno, con acceso al
 * comprobante. El administrador puede anular un ingreso o gasto mientras la caja sigue abierta (BR-035).
 */
export function ShiftOperations({ cashSessionId, refreshKey, onChanged }: {
  cashSessionId: string
  refreshKey: number
  onChanged?: () => void
}) {
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()
  const [operations, setOperations] = useState<CashOperation[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [voiding, setVoiding] = useState<CashOperation | null>(null)
  const [reason, setReason] = useState('')
  const [voidError, setVoidError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function load() {
    api
      .get<CashOperation[]>(`/cash-sessions/${cashSessionId}/payments`)
      .then(setOperations)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las operaciones'))
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [cashSessionId, refreshKey])

  async function confirmVoid() {
    if (!voiding) return
    if (!reason.trim()) {
      setVoidError('Indica el motivo de la anulación')
      return
    }
    setSubmitting(true)
    setVoidError(null)
    try {
      await api.post(`/cash-transactions/${voiding.paymentId}/void`, { reason: reason.trim() })
      toast(voiding.kind === 'EXPENSE' ? 'Gasto anulado' : 'Ingreso anulado')
      setVoiding(null)
      load()
      onChanged?.()
    } catch (err) {
      setVoidError(err instanceof ApiError ? err.message : 'No se pudo anular el movimiento')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card
      title="Operaciones de mi turno"
      description="Cobros, otros ingresos y gastos de esta caja, del más reciente al más antiguo."
      className="mt-4"
    >
      {error ? (
        <Alert>{error}</Alert>
      ) : operations === null ? (
        <Skeleton className="h-16" />
      ) : operations.length === 0 ? (
        <p className="text-sm text-neutral-500">Aún no hay operaciones en este turno.</p>
      ) : (
        <ul className="divide-y divide-neutral-100">
          {operations.map((op) => {
            const isMovement = op.kind === 'INCOME' || op.kind === 'EXPENSE'
            const voided = op.status === 'VOIDED'
            return (
              <li key={op.paymentId} className="flex flex-wrap items-center gap-3 py-3">
                {op.plate && <PlateBadge plate={op.plate} size="sm" />}
                {op.kind === 'INCOME' && <Badge tone="success">Otro ingreso</Badge>}
                {op.kind === 'EXPENSE' && <Badge tone="warning">Gasto</Badge>}
                <div className="min-w-0 flex-1 text-sm">
                  <p className="font-mono text-neutral-700">
                    {op.operationNumber}
                    {voided && (
                      <span className="ml-2">
                        <Badge tone="neutral">Anulado</Badge>
                      </span>
                    )}
                  </p>
                  <p className="text-neutral-500">
                    {formatTime(op.paidAt)} · {op.paymentMethodName ?? '—'}
                    {op.tariffName && <> · {op.tariffName}</>}
                  </p>
                  {voided && op.voidReason && <p className="text-neutral-500">Motivo: {op.voidReason}</p>}
                </div>
                <span
                  className={`font-semibold tabular-nums ${voided ? 'text-neutral-400 line-through' : op.kind === 'EXPENSE' ? 'text-danger-700' : 'text-neutral-900'}`}
                >
                  {op.kind === 'EXPENSE' ? '− ' : ''}
                  {formatCurrency(op.chargedAmount)}
                </span>
                <Button
                  variant="ghost"
                  aria-label={`Ver comprobante ${op.operationNumber}`}
                  icon={<Receipt aria-hidden className="size-4" />}
                  onClick={() => navigate(receiptPath(op))}
                >
                  Comprobante
                </Button>
                {isMovement && !voided && user?.role === 'ADMIN' && (
                  <Button
                    variant="ghost"
                    aria-label={`Anular ${op.operationNumber}`}
                    icon={<Ban aria-hidden className="size-4" />}
                    onClick={() => {
                      setReason('')
                      setVoidError(null)
                      setVoiding(op)
                    }}
                  >
                    Anular
                  </Button>
                )}
              </li>
            )
          })}
        </ul>
      )}

      <ConfirmDialog
        open={voiding !== null}
        title={`¿Anular ${voiding?.kind === 'EXPENSE' ? 'el gasto' : 'el ingreso'} ${voiding?.operationNumber ?? ''}?`}
        confirmLabel="Sí, anular"
        tone="danger"
        loading={submitting}
        onConfirm={confirmVoid}
        onCancel={() => setVoiding(null)}
      >
        <p>
          {formatCurrency(voiding?.chargedAmount ?? 0)} · {voiding?.tariffName}. Dejará de sumar en la caja y en los reportes, y
          queda en auditoría.
        </p>
        <div className="mt-4">
          <label className="label" htmlFor="void-reason">
            Motivo de la anulación
          </label>
          <textarea
            id="void-reason"
            className="input min-h-20"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={255}
            required
          />
          {voidError && <p className="mt-2 text-danger-700">{voidError}</p>}
        </div>
      </ConfirmDialog>
    </Card>
  )
}

/**
 * RF-081 / BR-032: el administrador registra gastos (salidas de caja) en cualquier caja abierta, la
 * suya o la de un empleado. Un gasto en efectivo no puede superar el efectivo disponible.
 */
export function CashExpensesAdmin() {
  const toast = useToast()
  const [sessions, setSessions] = useState<OpenCashSession[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<OpenCashSession | null>(null)

  function load() {
    api
      .get<OpenCashSession[]>('/cash-sessions/open')
      .then(setSessions)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las cajas abiertas'))
  }

  useEffect(load, [])

  return (
    <Card
      title="Gastos de caja"
      description="Registra una salida de dinero desde una caja abierta. Queda con comprobante y en auditoría."
      className="mt-6"
    >
      {error ? (
        <Alert>{error}</Alert>
      ) : sessions === null ? (
        <Skeleton className="h-16" />
      ) : sessions.length === 0 ? (
        <p className="text-sm text-neutral-500">No hay cajas abiertas. Se necesita una caja abierta para registrar un gasto.</p>
      ) : (
        <ul className="divide-y divide-neutral-100">
          {sessions.map((s) => (
            <li key={s.id} className="flex flex-wrap items-center gap-3 py-3">
              <div className="min-w-0 flex-1 text-sm">
                <p className="font-medium text-neutral-900">{s.employeeName ?? 'Empleado'}</p>
                <p className="text-neutral-500 tabular-nums">
                  Abierta {formatDateTime(s.openedAt)} · Efectivo disponible {formatCurrency(s.expectedCash)}
                </p>
              </div>
              <Button variant="secondary" icon={<MinusCircle aria-hidden className="size-4" />} onClick={() => setSelected(s)}>
                Registrar gasto
              </Button>
            </li>
          ))}
        </ul>
      )}

      <CashTransactionDialog
        type={selected ? 'EXPENSE' : null}
        cashSession={selected}
        onClose={() => setSelected(null)}
        onRegistered={() => {
          toast('Gasto registrado')
          load()
        }}
      />
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
