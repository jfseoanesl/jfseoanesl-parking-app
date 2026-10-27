import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Ban, ChartColumn, PlusCircle, Receipt, Wallet } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashSession, CashTransaction, CashTransactionReport } from '../api/types'
import { formatCurrency, formatTime, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, LoadingState, Skeleton } from '../components/Feedback'
import { Card, PageHeader } from '../components/Layout'
import { Badge } from '../components/Badges'
import { CashTransactionDialog } from '../components/CashTransactionDialog'
import { CashConceptCatalog } from '../components/CashConceptCatalog'
import { useAuth } from '../auth/AuthContext'
import { CashExpensesAdmin, ShiftOperations, VoidMovementDialog, type VoidTarget } from './CashSections'

/**
 * UC-014: acceso desde el menú a otros ingresos y gastos de caja. Todos registran otros ingresos en su
 * caja abierta y ven sus movimientos; el administrador además registra gastos, ve y anula los
 * movimientos de hoy de todas las cajas y administra los conceptos.
 */
export default function CashTransactionsPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [session, setSession] = useState<CashSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [registeringIncome, setRegisteringIncome] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    api
      .get<CashSession>('/cash-sessions/current')
      .then(setSession)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) setSession(null)
        else setError(err instanceof ApiError ? err.message : 'No se pudo cargar la caja')
      })
      .finally(() => setLoading(false))
  }, [])

  const refresh = () => setRefreshKey((n) => n + 1)

  if (loading) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="Ingresos y gastos"
        description={isAdmin ? 'Otros ingresos y gastos de caja, con comprobante y auditoría.' : 'Otros ingresos registrados en tu caja.'}
        actions={
          <Button icon={<PlusCircle aria-hidden className="size-5" />} disabled={!session} onClick={() => setRegisteringIncome(true)}>
            Registrar otro ingreso
          </Button>
        }
      />

      {error && <Alert className="mb-4">{error}</Alert>}
      {!session && !error && (
        <Alert tone="warning" className="mb-4">
          <span className="flex flex-wrap items-center gap-2">
            <Wallet aria-hidden className="size-4" />
            Abre tu caja para registrar otros ingresos.
            <Link to="/caja" className="font-medium underline">
              Ir a Caja
            </Link>
          </span>
        </Alert>
      )}

      {session && <ShiftOperations cashSessionId={session.id} refreshKey={refreshKey} onChanged={refresh} onlyMovements />}

      {isAdmin && (
        <>
          <CashExpensesAdmin refreshKey={refreshKey} onRegistered={refresh} />
          <TodayMovementsAdmin refreshKey={refreshKey} onChanged={refresh} />
          <div className="mt-6 lg:max-w-xl">
            <CashConceptCatalog />
          </div>
        </>
      )}

      <CashTransactionDialog
        type={registeringIncome ? 'INCOME' : null}
        onClose={() => setRegisteringIncome(false)}
        onRegistered={refresh}
      />
    </div>
  )
}

/** RF-083: movimientos de hoy de todas las cajas, con comprobante y anulación (solo administrador). */
function TodayMovementsAdmin({ refreshKey, onChanged }: { refreshKey: number; onChanged: () => void }) {
  const navigate = useNavigate()
  const [report, setReport] = useState<CashTransactionReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [voiding, setVoiding] = useState<VoidTarget | null>(null)

  useEffect(() => {
    const today = localIsoDate()
    api
      .get<CashTransactionReport>(`/reports/cash-transactions?from=${today}&to=${today}`)
      .then(setReport)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los movimientos'))
  }, [refreshKey])

  return (
    <Card
      title="Movimientos de hoy"
      description="Otros ingresos y gastos de todas las cajas registrados hoy."
      className="mt-6"
      actions={
        <Link to="/reportes" className="inline-flex items-center gap-1.5 text-sm font-medium text-primary-700 hover:underline">
          <ChartColumn aria-hidden className="size-4" /> Ver reporte completo
        </Link>
      }
    >
      {error ? (
        <Alert>{error}</Alert>
      ) : report === null ? (
        <Skeleton className="h-16" />
      ) : report.transactions.length === 0 ? (
        <p className="text-sm text-neutral-500">Hoy no se han registrado otros ingresos ni gastos.</p>
      ) : (
        <>
          <p className="mb-2 text-sm text-neutral-600 tabular-nums">
            Ingresos {formatCurrency(report.incomeTotal)} · Gastos {formatCurrency(report.expensesTotal)} · Neto{' '}
            <strong className="text-neutral-900">{formatCurrency(report.net)}</strong>
          </p>
          <ul className="divide-y divide-neutral-100">
            {report.transactions.map((t) => (
              <MovementRow key={t.id} transaction={t} onReceipt={() => navigate(`/comprobantes-movimiento/${t.id}`)} onVoid={() =>
                setVoiding({
                  id: t.id,
                  type: t.type,
                  operationNumber: t.operationNumber,
                  amount: t.amount,
                  label: t.description ? `${t.conceptName} - ${t.description}` : t.conceptName,
                })
              } />
            ))}
          </ul>
        </>
      )}

      <VoidMovementDialog target={voiding} onClose={() => setVoiding(null)} onVoided={onChanged} />
    </Card>
  )
}

function MovementRow({ transaction: t, onReceipt, onVoid }: { transaction: CashTransaction; onReceipt: () => void; onVoid: () => void }) {
  const voided = t.status === 'VOIDED'
  return (
    <li className="flex flex-wrap items-center gap-3 py-3">
      <Badge tone={t.type === 'EXPENSE' ? 'warning' : 'success'}>{t.type === 'EXPENSE' ? 'Gasto' : 'Otro ingreso'}</Badge>
      <div className="min-w-0 flex-1 text-sm">
        <p className="font-mono text-neutral-700">
          {t.operationNumber}
          {voided && (
            <span className="ml-2">
              <Badge tone="neutral">Anulado</Badge>
            </span>
          )}
        </p>
        <p className="text-neutral-500">
          {formatTime(t.createdAt)} · {t.conceptName}
          {t.description && <> - {t.description}</>} · {t.paymentMethodName ?? '—'}
        </p>
        <p className="text-neutral-500">
          Registró {t.userName ?? '—'}
          {t.cashierName && <> · caja de {t.cashierName}</>}
        </p>
        {voided && t.voidReason && <p className="text-neutral-500">Motivo: {t.voidReason}</p>}
      </div>
      <span
        className={`font-semibold tabular-nums ${voided ? 'text-neutral-400 line-through' : t.type === 'EXPENSE' ? 'text-danger-700' : 'text-neutral-900'}`}
      >
        {t.type === 'EXPENSE' ? '− ' : ''}
        {formatCurrency(t.amount)}
      </span>
      <Button variant="ghost" aria-label={`Ver comprobante ${t.operationNumber}`} icon={<Receipt aria-hidden className="size-4" />} onClick={onReceipt}>
        Comprobante
      </Button>
      {!voided && (
        <Button variant="ghost" aria-label={`Anular ${t.operationNumber}`} icon={<Ban aria-hidden className="size-4" />} onClick={onVoid}>
          Anular
        </Button>
      )}
    </li>
  )
}
