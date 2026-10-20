import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowRightLeft, Banknote, CalendarCheck, CircleCheck, CreditCard, Info, LogOut, Printer, Tag, Wallet } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CheckoutResult, ParkingConfiguration, ParkingSession, PaymentMethod, SubscriptionCoverage, TariffOption, TariffQuote } from '../api/types'
import { DEFAULT_RECEIPT_FORMAT, parseReceiptFormat } from '../lib/receiptFormat'
import { formatCurrency, formatDate, formatElapsed, formatTime } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, LoadingState, Skeleton } from '../components/Feedback'
import { ChoiceGroup, TextField } from '../components/Form'
import { SummaryRow } from '../components/Layout'
import { PlateBadge, VehicleIcon } from '../components/Badges'
import { ConfirmDialog } from '../components/ConfirmDialog'

function paymentIcon(code: string): ReactNode {
  if (code === 'CASH') return <Banknote aria-hidden className="size-5" />
  if (code === 'TRANSFER') return <ArrowRightLeft aria-hidden className="size-5" />
  return <CreditCard aria-hidden className="size-5" />
}

/** UC-004 / UC-005 (RF-034 a RF-051). Wireframe: 09-ux-specification.md §5.5. */
export default function CheckoutPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()

  const [session, setSession] = useState<ParkingSession | null>(null)
  const [quote, setQuote] = useState<TariffQuote | null>(null)
  /** BR-008.1: tarifa elegida entre las aplicables (arranca en la sugerida). */
  const [tariffId, setTariffId] = useState('')
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([])
  const [paymentMethodId, setPaymentMethodId] = useState('')
  const [error, setError] = useState<string | null>(null)
  /** El valor cambio entre cotizar y confirmar (CHARGED_AMOUNT_MISMATCH): se re-cotizo. */
  const [amountUpdated, setAmountUpdated] = useState(false)
  const [needsCashSession, setNeedsCashSession] = useState(false)
  const [openingBalance, setOpeningBalance] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [result, setResult] = useState<CheckoutResult | null>(null)
  const [printOnExit, setPrintOnExit] = useState(DEFAULT_RECEIPT_FORMAT.printOnExit)

  const selectedTariff = quote?.options.find((o) => o.tariffId === tariffId)
  const tariffChanged = selectedTariff != null && !selectedTariff.suggested

  useEffect(() => {
    if (!sessionId) return
    api.get<ParkingSession>(`/parking-sessions/${sessionId}`).then(setSession)
    api
      .get<ParkingConfiguration>('/configuration')
      .then((c) => setPrintOnExit(parseReceiptFormat(c.receiptFormatJson).printOnExit))
      .catch(() => undefined)
    api.get<PaymentMethod[]>('/payment-methods').then((methods) => {
      setPaymentMethods(methods)
      if (methods.length > 0) setPaymentMethodId(methods[0].id)
    })
    requestQuote()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId])

  /** @param keepTariffId conserva la tarifa elegida si sigue siendo aplicable (re-cotizacion). */
  function requestQuote(keepTariffId?: string) {
    if (!sessionId) return
    setError(null)
    setNeedsCashSession(false)
    api
      .post<TariffQuote>(`/parking-sessions/${sessionId}/quote`)
      .then((q) => {
        setQuote(q)
        const stillApplicable = keepTariffId && q.options.some((o) => o.tariffId === keepTariffId)
        setTariffId(stillApplicable ? keepTariffId : (q.tariffId ?? ''))
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo calcular la tarifa'))
  }

  async function handleOpenCashSession(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await api.post('/cash-sessions/open', { initialBalance: Number(openingBalance) })
      setNeedsCashSession(false)
      setOpeningBalance('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo abrir la caja')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCheckout() {
    if (!sessionId || !quote) return
    setError(null)
    setAmountUpdated(false)
    setSubmitting(true)
    try {
      const checkoutResult = await api.post<CheckoutResult>(`/parking-sessions/${sessionId}/checkout`, {
        paymentMethodId,
        // El backend recalcula el valor con la tarifa elegida y rechaza una que no aplique.
        chargedAmount: selectedTariff?.calculatedAmount ?? quote.calculatedAmount,
        tariffId,
      })
      setResult(checkoutResult)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'NO_OPEN_CASH_SESSION') {
        setNeedsCashSession(true)
      } else if (err instanceof ApiError && err.code === 'CHARGED_AMOUNT_MISMATCH') {
        // Paso una fraccion de tiempo (u otra condicion cambio) desde la cotizacion: se muestra el
        // valor nuevo y el usuario debe confirmarlo otra vez. Nunca se cobra un valor no mostrado.
        requestQuote(tariffId)
        setAmountUpdated(true)
      } else {
        setError(err instanceof ApiError ? err.message : 'No se pudo registrar el cobro')
      }
    } finally {
      setSubmitting(false)
      setConfirming(false)
    }
  }

  if (result) return <Receipt result={result} onDone={() => navigate('/parqueados')} onPrint={printOnExit ? () => navigate(`/comprobantes/${result.paymentId}`) : undefined} />
  if (!session) return <LoadingState />
  // BR-030: cliente mensual al dia o en gracia -> salida sin cobro ni medio de pago.
  if (quote?.subscription?.covered) {
    return <CoveredExit session={session} coverage={quote.subscription} elapsedMinutes={quote.elapsedMinutes} />
  }

  const selectedMethod = paymentMethods.find((m) => m.id === paymentMethodId)

  return (
    <div className="mx-auto max-w-lg">
      <button
        type="button"
        onClick={() => navigate('/parqueados')}
        className="mb-4 inline-flex min-h-11 items-center gap-2 rounded-lg text-sm font-medium text-neutral-600 transition-colors hover:text-neutral-900"
      >
        <ArrowLeft aria-hidden className="size-4" />
        Volver a parqueados
      </button>

      <div className="card overflow-hidden">
        <div className="border-b border-neutral-100 p-5 sm:p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <PlateBadge plate={session.plate} size="lg" />
            <span className="flex items-center gap-2 text-sm text-neutral-600">
              <VehicleIcon code={session.vehicleTypeCode} />
              {session.vehicleTypeName}
            </span>
          </div>
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-neutral-500">Ingreso</dt>
              <dd className="font-medium tabular-nums text-neutral-900">{formatTime(session.entryDateTime)}</dd>
            </div>
            <div>
              <dt className="text-neutral-500">Permanencia</dt>
              <dd className="font-medium tabular-nums text-neutral-900">
                {formatElapsed(quote?.elapsedMinutes ?? session.elapsedMinutes)}
              </dd>
            </div>
          </dl>
        </div>

        <div className="space-y-6 p-5 sm:p-6">
          {quote?.subscription && !quote.subscription.covered && (
            <Alert tone="warning">
              La mensualidad de {quote.subscription.customerName} está{' '}
              {quote.subscription.status === 'INACTIVA' ? 'inactiva' : quote.subscription.status === 'SIN_PAGO' ? 'sin pago' : 'vencida'}
              {quote.subscription.paidUntil && <> (pagada hasta {formatDate(quote.subscription.paidUntil)})</>}. Esta salida se cobra como
              cliente normal.
            </Alert>
          )}
          <div className="rounded-xl bg-neutral-900 p-5 text-white">
            <p className="text-sm text-neutral-400">Valor a cobrar</p>
            {quote ? (
              <>
                <p className="mt-1 text-4xl font-semibold tabular-nums tracking-tight">
                  {formatCurrency(selectedTariff?.calculatedAmount ?? quote.calculatedAmount)}
                </p>
                <p className="mt-2 text-sm text-neutral-400">
                  Tarifa: {selectedTariff?.tariffName ?? quote.tariffName}
                  {tariffChanged && <span className="ml-2 rounded bg-warning-500/20 px-1.5 py-0.5 text-xs text-warning-200">cambiada</span>}
                </p>
              </>
            ) : (
              <Skeleton className="mt-2 h-10 w-40 bg-white/10" />
            )}
          </div>

          {/* BR-008.1: la seccion Tarifa se muestra siempre, para que se sepa que la tarifa puede
              cambiarse; con una sola aplicable se muestra fija y se explica por que. */}
          {quote && quote.options.length === 1 && (
            <SingleTariff option={quote.options[0]} longStay={quote.elapsedMinutes > 24 * 60} />
          )}
          {quote && quote.options.length > 1 && (
            <ChoiceGroup
              label="Tarifa"
              value={tariffId}
              onChange={(id) => {
                setTariffId(id)
                setAmountUpdated(false)
              }}
              options={quote.options.map((option) => ({
                value: option.tariffId,
                label: option.tariffName,
                icon: <Tag aria-hidden className="size-5" />,
                description: `${formatCurrency(option.calculatedAmount)}${option.suggested ? ' · Sugerida' : ''}`,
              }))}
            />
          )}

          <ChoiceGroup
            label="Medio de pago"
            columns={3}
            value={paymentMethodId}
            onChange={setPaymentMethodId}
            options={paymentMethods.map((method) => ({
              value: method.id,
              label: method.name,
              icon: paymentIcon(method.code),
            }))}
          />

          {needsCashSession && (
            <form onSubmit={handleOpenCashSession} className="space-y-3 rounded-xl border border-warning-200 bg-warning-50 p-4">
              <p className="flex items-center gap-2 text-sm font-medium text-warning-800">
                <Wallet aria-hidden className="size-5" />
                Debe abrir caja antes de cobrar.
              </p>
              <TextField
                id="initialBalance"
                label="Saldo inicial"
                type="number"
                inputMode="numeric"
                min="0"
                adornment="$"
                value={openingBalance}
                onChange={(e) => setOpeningBalance(e.target.value)}
                required
              />
              <Button type="submit" loading={submitting} fullWidth>
                Abrir caja
              </Button>
            </form>
          )}

          {amountUpdated && (
            <Alert tone="warning">
              El valor a cobrar se actualizó porque cambió el tiempo de permanencia. Revísalo y confirma de nuevo.
            </Alert>
          )}
          {error && <Alert>{error}</Alert>}

          <Button
            size="lg"
            variant="success"
            fullWidth
            onClick={() => setConfirming(true)}
            disabled={submitting || !quote || !paymentMethodId || needsCashSession}
          >
            Confirmar cobro
          </Button>
        </div>
      </div>

      <ConfirmDialog
        open={confirming}
        title="¿Registrar el cobro?"
        confirmLabel="Sí, cobrar"
        tone="success"
        loading={submitting}
        onConfirm={handleCheckout}
        onCancel={() => setConfirming(false)}
      >
        Se cobrará <strong className="text-neutral-900">{selectedTariff ? formatCurrency(selectedTariff.calculatedAmount) : ''}</strong>{' '}
        con la tarifa <strong className="text-neutral-900">{selectedTariff?.tariffName}</strong>
        {tariffChanged && <> (en lugar de la sugerida, {quote?.tariffName})</>} en{' '}
        <strong className="text-neutral-900">{selectedMethod?.name}</strong> y se registrará la salida del vehículo{' '}
        <strong className="font-mono text-neutral-900">{session.plate}</strong>. Esta operación no se puede deshacer.
      </ConfirmDialog>
    </div>
  )
}

/** Seccion Tarifa cuando solo hay una aplicable: visible, pero sin opcion de cambio. */
function SingleTariff({ option, longStay }: { option: TariffOption; longStay: boolean }) {
  return (
    <fieldset>
      <legend className="label">Tarifa</legend>
      <div className="flex min-h-12 items-center gap-2.5 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm">
        <Tag aria-hidden className="size-5 text-neutral-500" />
        <span className="min-w-0 flex-1 font-medium text-neutral-800">
          {option.tariffName}
          <span className="block text-xs font-normal text-neutral-500">
            {formatCurrency(option.calculatedAmount)} · Única aplicable
          </span>
        </span>
      </div>
      <p className="mt-1.5 flex items-start gap-1.5 text-xs text-neutral-600">
        <Info aria-hidden className="mt-px size-3.5 shrink-0" />
        {longStay
          ? 'Permanencia mayor a 24 horas: se cobra por días completos más el tiempo restante (BR-015), por eso no se puede cambiar la tarifa.'
          : 'Es la única tarifa aplicable a este vehículo en este horario, por eso no hay otra para elegir.'}
      </p>
    </fieldset>
  )
}

/** Comprobante de pago (RF-048): confirmacion clara del cobro realizado. */
function Receipt({ result, onDone, onPrint }: { result: CheckoutResult; onDone: () => void; onPrint?: () => void }) {
  return (
    <div className="mx-auto max-w-md">
      <div className="card overflow-hidden text-center">
        <div className="bg-success-50 px-6 pt-8 pb-6">
          <CircleCheck aria-hidden className="mx-auto size-14 text-success-600" />
          <p className="mt-3 text-lg font-semibold text-success-800">Cobro registrado</p>
          <p className="mt-4 text-4xl font-semibold tabular-nums text-neutral-900">{formatCurrency(result.chargedAmount)}</p>
        </div>
        <div className="border-t border-dashed border-neutral-300 px-6 py-4 text-left">
          <div className="mb-2 flex justify-center">
            <PlateBadge plate={result.plate} />
          </div>
          <dl className="divide-y divide-neutral-100">
            <SummaryRow label="Medio de pago" value={result.paymentMethodName} />
            <SummaryRow label="Operación" value={<span className="font-mono">{result.operationNumber}</span>} />
            <SummaryRow label="Tarifa" value={result.appliedTariffName} />
            <SummaryRow label="Ingreso → salida" value={`${formatTime(result.entryDateTime)} → ${formatTime(result.exitDateTime)}`} />
          </dl>
        </div>
        <div className="flex flex-col gap-2 border-t border-neutral-100 p-4">
          <Button size="lg" fullWidth onClick={onDone}>
            Volver a parqueados
          </Button>
          {/* RF-015: solo si la configuración pide imprimir comprobante a la salida. */}
          {onPrint && (
            <Button variant="secondary" fullWidth icon={<Printer aria-hidden className="size-5" />} onClick={onPrint}>
              Ver comprobante para imprimir
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}

/** RF-076 / BR-030: salida de un cliente mensual cubierto, sin cobro. */
function CoveredExit({
  session,
  coverage,
  elapsedMinutes,
}: {
  session: ParkingSession
  coverage: SubscriptionCoverage
  elapsedMinutes: number
}) {
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  async function handleExit() {
    setSubmitting(true)
    setError(null)
    try {
      await api.post(`/parking-sessions/${session.id}/exit-subscription`)
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar la salida')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="card overflow-hidden text-center">
        <div className="bg-primary-50 px-6 pt-8 pb-6">
          {done ? (
            <CircleCheck aria-hidden className="mx-auto size-14 text-success-600" />
          ) : (
            <CalendarCheck aria-hidden className="mx-auto size-14 text-primary-600" />
          )}
          <p className="mt-3 text-lg font-semibold text-primary-900">
            {done ? 'Salida registrada' : 'Cubierto por mensualidad'}
          </p>
          <p className="mt-1 text-sm text-neutral-700">
            {coverage.customerName}
            {coverage.paidUntil && <> · vigente hasta {formatDate(coverage.paidUntil)}</>}
          </p>
          {coverage.status === 'EN_GRACIA' && (
            <p className="mt-2 text-sm font-medium text-warning-800">En días de gracia: recuerde al cliente pagar la mensualidad.</p>
          )}
        </div>
        <div className="border-t border-dashed border-neutral-300 px-6 py-4 text-left">
          <div className="mb-2 flex justify-center">
            <PlateBadge plate={session.plate} />
          </div>
          <dl className="divide-y divide-neutral-100">
            <SummaryRow label="Ingreso" value={formatTime(session.entryDateTime)} />
            <SummaryRow label="Permanencia" value={formatElapsed(elapsedMinutes)} />
            <SummaryRow label="Valor a cobrar" value={formatCurrency(0)} />
          </dl>
        </div>
        {error && (
          <div className="px-4 pb-2">
            <Alert>{error}</Alert>
          </div>
        )}
        <div className="flex flex-col gap-2 border-t border-neutral-100 p-4">
          {done ? (
            <Button size="lg" fullWidth onClick={() => navigate('/parqueados')}>
              Volver a parqueados
            </Button>
          ) : (
            <>
              <Button size="lg" fullWidth loading={submitting} icon={<LogOut aria-hidden className="size-5" />} onClick={handleExit}>
                Registrar salida sin cobro
              </Button>
              <Button variant="secondary" fullWidth onClick={() => navigate('/parqueados')}>
                Cancelar
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

