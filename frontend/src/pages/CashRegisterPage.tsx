import { useEffect, useState, type FormEvent } from 'react'
import { ArrowRightLeft, Banknote, Car, CircleCheck, CreditCard, Lock, PlusCircle, Wallet } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashSession, CashSessionSummary } from '../api/types'
import { formatCurrency, formatTime } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, LoadingState } from '../components/Feedback'
import { TextField } from '../components/Form'
import { Card, PageHeader, StatCard, SummaryRow } from '../components/Layout'
import { Badge } from '../components/Badges'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useAuth } from '../auth/AuthContext'
import { CashExpensesAdmin, ClosedCashSessionsAdmin, ShiftOperations } from './CashSections'
import { CashTransactionDialog } from '../components/CashTransactionDialog'

/** Diferencia de arqueo: 0 cuadra, negativa falta dinero, positiva sobra (BR-023.1). */
function differenceStyle(diff: number): { className: string; label: string } {
  if (diff === 0) return { className: 'text-success-700', label: 'Cuadra' }
  if (diff < 0) return { className: 'text-danger-700', label: 'Faltante' }
  return { className: 'text-warning-700', label: 'Sobrante' }
}

/**
 * UC-006 (apertura, Fase 5) y UC-007 (cierre/arqueo, Fase 6). Wireframe: 09-ux-specification.md §5.6.
 * El Administrador ve además las cajas cerradas para corregirlas (RF-057).
 */
export default function CashRegisterPage() {
  const { user } = useAuth()
  return (
    <>
      <CurrentCashRegister />
      {user?.role === 'ADMIN' && (
        <>
          <CashExpensesAdmin />
          <ClosedCashSessionsAdmin />
        </>
      )}
    </>
  )
}

function CurrentCashRegister() {
  const [session, setSession] = useState<CashSession | null>(null)
  const [summary, setSummary] = useState<CashSessionSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [confirmingClose, setConfirmingClose] = useState(false)

  const [openingBalance, setOpeningBalance] = useState('')
  const [reportedCash, setReportedCash] = useState('')
  const [closed, setClosed] = useState<CashSession | null>(null)
  const [registeringIncome, setRegisteringIncome] = useState(false)
  const [movements, setMovements] = useState(0)

  function loadCurrent() {
    api
      .get<CashSession>('/cash-sessions/current')
      .then((current) => {
        setSession(current)
        return api.get<CashSessionSummary>(`/cash-sessions/${current.id}/summary`)
      })
      .then((s) => s && setSummary(s))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) {
          setSession(null)
        } else {
          setError(err instanceof ApiError ? err.message : 'No se pudo cargar la caja')
        }
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadCurrent()
    const interval = setInterval(loadCurrent, 20000)
    return () => clearInterval(interval)
  }, [])

  async function handleOpen(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.post('/cash-sessions/open', { initialBalance: Number(openingBalance) })
      setOpeningBalance('')
      loadCurrent()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo abrir la caja')
    } finally {
      setSubmitting(false)
    }
  }

  function requestClose(event: FormEvent) {
    event.preventDefault()
    setConfirmingClose(true)
  }

  async function handleClose() {
    if (!session) return
    setError(null)
    setSubmitting(true)
    try {
      const result = await api.post<CashSession>(`/cash-sessions/${session.id}/close`, {
        reportedCash: Number(reportedCash),
      })
      setClosed(result)
      setSession(null)
      setSummary(null)
      setReportedCash('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cerrar la caja')
    } finally {
      setSubmitting(false)
      setConfirmingClose(false)
    }
  }

  if (loading) return <LoadingState />

  if (closed) {
    const diff = closed.difference ?? 0
    const style = differenceStyle(diff)
    return (
      <div className="mx-auto max-w-md">
        <div className="card overflow-hidden">
          <div className="bg-success-50 px-6 py-8 text-center">
            <CircleCheck aria-hidden className="mx-auto size-14 text-success-600" />
            <p className="mt-3 text-lg font-semibold text-success-800">Caja cerrada</p>
            <p className="mt-1 text-sm text-neutral-600">
              {closed.closedAt && `Cerrada a las ${formatTime(closed.closedAt)}`}
            </p>
          </div>
          <dl className="divide-y divide-neutral-100 px-6 py-2">
            <SummaryRow label="Saldo inicial" value={formatCurrency(closed.initialBalance)} />
            <SummaryRow label="Total recaudado" value={formatCurrency(closed.totalCollected ?? 0)} />
            {closed.otherIncomeCollected > 0 && (
              <SummaryRow label="Incluye otros ingresos" value={formatCurrency(closed.otherIncomeCollected)} />
            )}
            {closed.expensesTotal > 0 && <SummaryRow label="Gastos de caja" value={`− ${formatCurrency(closed.expensesTotal)}`} />}
            <SummaryRow label="Efectivo esperado" value={formatCurrency(closed.expectedCash ?? 0)} />
            <SummaryRow label="Efectivo reportado" value={formatCurrency(closed.reportedCash ?? 0)} />
            <SummaryRow
              emphasis
              label={
                <span className="flex items-center gap-2">
                  Diferencia <Badge tone={diff === 0 ? 'success' : diff < 0 ? 'danger' : 'warning'}>{style.label}</Badge>
                </span>
              }
              value={formatCurrency(diff)}
              valueClassName={style.className}
            />
          </dl>
          <div className="border-t border-neutral-100 p-4">
            <Button size="lg" fullWidth onClick={() => setClosed(null)}>
              Listo
            </Button>
          </div>
        </div>
      </div>
    )
  }

  if (!session) {
    return (
      <div className="mx-auto max-w-md">
        <PageHeader title="Abrir caja" description="Registra el dinero con el que inicias el turno." />
        <form onSubmit={handleOpen} className="card space-y-5 p-5 sm:p-6">
          <div className="flex items-center gap-3 rounded-xl bg-primary-50 p-4 text-sm text-primary-800">
            <Wallet aria-hidden className="size-6 shrink-0 text-primary-600" />
            Necesitas una caja abierta para poder registrar cobros.
          </div>
          <TextField
            id="openingBalance"
            label="Saldo inicial"
            type="number"
            inputMode="numeric"
            min="0"
            adornment="$"
            hint="Efectivo en la caja al empezar (puede ser 0)."
            value={openingBalance}
            onChange={(e) => setOpeningBalance(e.target.value)}
            required
          />
          {error && <Alert>{error}</Alert>}
          <Button type="submit" size="lg" fullWidth loading={submitting}>
            Abrir caja
          </Button>
        </form>
      </div>
    )
  }

  const reported = reportedCash === '' ? null : Number(reportedCash)
  const previewDiff = summary && reported != null ? reported - summary.expectedCash : null
  const previewStyle = previewDiff != null ? differenceStyle(previewDiff) : null

  return (
    <div>
      <PageHeader
        title="Caja"
        description={
          <span className="inline-flex items-center gap-2">
            <Badge tone="success">Abierta</Badge> desde las {formatTime(session.openedAt)}
          </span>
        }
        actions={
          <Button variant="secondary" icon={<PlusCircle aria-hidden className="size-5" />} onClick={() => setRegisteringIncome(true)}>
            Registrar otro ingreso
          </Button>
        }
      />

      {summary && (
        <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard label="Total recaudado" value={formatCurrency(summary.totalCollected)} icon={<Wallet aria-hidden className="size-5" />} tone="success" />
          <StatCard label="Efectivo" value={formatCurrency(summary.cashCollected)} icon={<Banknote aria-hidden className="size-5" />} />
          <StatCard label="Electrónico" value={formatCurrency(summary.electronicCollected)} icon={<ArrowRightLeft aria-hidden className="size-5" />} />
          <StatCard label="Vehículos atendidos" value={summary.vehiclesServed} icon={<Car aria-hidden className="size-5" />} tone="neutral" />
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        {summary && (
          <Card title="Resumen del turno">
            <dl className="divide-y divide-neutral-100">
              <SummaryRow label="Saldo inicial" value={formatCurrency(summary.initialBalance)} />
              <SummaryRow label="Cobros en efectivo" value={formatCurrency(summary.cashCollected)} />
              <SummaryRow label="Cobros electrónicos" value={formatCurrency(summary.electronicCollected)} />
              <SummaryRow
                label={
                  <span className="flex items-center gap-1.5">
                    <CreditCard aria-hidden className="size-4" /> Otros medios
                  </span>
                }
                value={formatCurrency(summary.otherCollected)}
              />
              {summary.otherIncomeCollected > 0 && (
                <SummaryRow label="Incluye otros ingresos" value={formatCurrency(summary.otherIncomeCollected)} />
              )}
              {summary.expensesTotal > 0 && (
                <SummaryRow
                  label="Gastos de caja"
                  value={`− ${formatCurrency(summary.expensesTotal)}`}
                  valueClassName="text-danger-700"
                />
              )}
              {summary.expensesCash !== summary.expensesTotal && (
                <SummaryRow label="Gastos en efectivo" value={`− ${formatCurrency(summary.expensesCash)}`} />
              )}
              <SummaryRow emphasis label="Efectivo esperado en caja" value={formatCurrency(summary.expectedCash)} />
            </dl>
          </Card>
        )}

        <Card title="Cerrar caja" description="Cuenta el efectivo físico y regístralo.">
          <form onSubmit={requestClose} className="space-y-4">
            <TextField
              id="reportedCash"
              label="Efectivo reportado (conteo físico)"
              type="number"
              inputMode="numeric"
              min="0"
              adornment="$"
              value={reportedCash}
              onChange={(e) => setReportedCash(e.target.value)}
              required
            />
            {previewDiff != null && previewStyle && (
              <div aria-live="polite" className="flex items-center justify-between rounded-lg bg-neutral-50 px-4 py-3 text-sm">
                <span className="text-neutral-600">Diferencia</span>
                <span className={`font-semibold tabular-nums ${previewStyle.className}`}>
                  {formatCurrency(previewDiff)} · {previewStyle.label}
                </span>
              </div>
            )}
            {error && <Alert>{error}</Alert>}
            <Button type="submit" variant="danger" size="lg" fullWidth icon={<Lock aria-hidden className="size-5" />}>
              Cerrar caja
            </Button>
          </form>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmingClose}
        title="¿Cerrar la caja?"
        confirmLabel="Sí, cerrar caja"
        tone="danger"
        loading={submitting}
        onConfirm={handleClose}
        onCancel={() => setConfirmingClose(false)}
      >
        Reportarás <strong className="text-neutral-900">{reported != null ? formatCurrency(reported) : ''}</strong> en
        efectivo
        {previewDiff != null && previewDiff !== 0 && (
          <>
            {' '}(diferencia de <strong className={previewStyle?.className}>{formatCurrency(previewDiff)}</strong>)
          </>
        )}
        . Después del cierre no podrás registrar más cobros en esta caja.
      </ConfirmDialog>

      <CashTransactionDialog
        type={registeringIncome ? 'INCOME' : null}
        onClose={() => setRegisteringIncome(false)}
        onRegistered={() => {
          setMovements((n) => n + 1)
          loadCurrent()
        }}
      />

      <ShiftOperations
        cashSessionId={session.id}
        refreshKey={(summary?.vehiclesServed ?? 0) + movements + (summary?.totalCollected ?? 0) + (summary?.expensesTotal ?? 0)}
        onChanged={() => {
          setMovements((n) => n + 1)
          loadCurrent()
        }}
      />
    </div>
  )
}
