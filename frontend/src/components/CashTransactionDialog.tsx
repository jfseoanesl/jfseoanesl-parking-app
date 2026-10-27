import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleCheck, Receipt } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashConcept, CashTransaction, CashTransactionType, OpenCashSession, PaymentMethod } from '../api/types'
import { formatCurrency } from '../lib/format'
import { Button } from './Button'
import { Alert } from './Feedback'
import { SelectField, TextField } from './Form'

const TITLES: Record<CashTransactionType, string> = {
  INCOME: 'Registrar otro ingreso',
  EXPENSE: 'Registrar gasto de caja',
}

/**
 * UC-014 (RF-080/RF-081): formulario de otro ingreso o gasto. Un ingreso va a la caja abierta de quien
 * lo registra; un gasto (solo administrador) a la caja elegida en `cashSession`. Al registrar muestra
 * el número de operación y ofrece el comprobante (RF-082).
 */
export function CashTransactionDialog({
  type,
  cashSession,
  onClose,
  onRegistered,
}: {
  /** null = cerrado. */
  type: CashTransactionType | null
  /** Solo para gastos: caja de la que sale el dinero. */
  cashSession?: OpenCashSession | null
  onClose: () => void
  onRegistered: () => void
}) {
  const navigate = useNavigate()
  const ref = useRef<HTMLDialogElement>(null)
  const [concepts, setConcepts] = useState<CashConcept[]>([])
  const [methods, setMethods] = useState<PaymentMethod[]>([])
  const [conceptId, setConceptId] = useState('')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [paymentMethodId, setPaymentMethodId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [registered, setRegistered] = useState<CashTransaction | null>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (type && !dialog.open) dialog.showModal()
    if (!type && dialog.open) dialog.close()
  }, [type])

  useEffect(() => {
    if (!type) return
    setDescription('')
    setAmount('')
    setError(null)
    setRegistered(null)
    Promise.all([
      api.get<CashConcept[]>(`/cash-concepts?type=${type}&active=true`),
      api.get<PaymentMethod[]>('/payment-methods'),
    ])
      .then(([c, m]) => {
        setConcepts(c)
        setMethods(m)
        setConceptId(c[0]?.id ?? '')
        setPaymentMethodId((m.find((x) => x.code === 'CASH') ?? m[0])?.id ?? '')
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los conceptos'))
  }, [type])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!type) return
    setError(null)
    setSubmitting(true)
    try {
      const result = await api.post<CashTransaction>('/cash-transactions', {
        type,
        conceptId,
        description: description.trim() || null,
        amount: Number(amount),
        paymentMethodId,
        cashSessionId: type === 'EXPENSE' ? cashSession?.id : null,
      })
      setRegistered(result)
      onRegistered()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar el movimiento')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby="cash-transaction-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!submitting) onClose()
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-md animate-fade-in rounded-2xl bg-white p-0 shadow-xl"
    >
      {type && registered ? (
        <div className="p-6 text-center">
          <CircleCheck aria-hidden className="mx-auto size-12 text-success-600" />
          <h2 id="cash-transaction-title" className="mt-3 text-lg font-semibold text-neutral-900">
            {type === 'EXPENSE' ? 'Gasto registrado' : 'Ingreso registrado'}
          </h2>
          <p className="mt-1 text-sm text-neutral-600">
            <span className="font-mono">{registered.operationNumber}</span> · {formatCurrency(registered.amount)}
          </p>
          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-center">
            <Button variant="secondary" onClick={onClose}>
              Listo
            </Button>
            <Button
              icon={<Receipt aria-hidden className="size-4" />}
              onClick={() => navigate(`/comprobantes-movimiento/${registered.id}`)}
            >
              Ver comprobante
            </Button>
          </div>
        </div>
      ) : (
        type && (
          <form onSubmit={handleSubmit}>
            <div className="space-y-4 p-6">
              <h2 id="cash-transaction-title" className="text-lg font-semibold text-neutral-900">
                {TITLES[type]}
              </h2>
              {type === 'EXPENSE' && cashSession && (
                <p className="text-sm text-neutral-600">
                  Sale de la caja de <strong className="text-neutral-900">{cashSession.employeeName ?? 'empleado'}</strong>{' '}
                  (efectivo disponible {formatCurrency(cashSession.expectedCash)}).
                </p>
              )}
              <SelectField label="Concepto" value={conceptId} onChange={(e) => setConceptId(e.target.value)} required>
                {concepts.length === 0 && <option value="">No hay conceptos activos</option>}
                {concepts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </SelectField>
              <TextField
                label="Descripción"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={255}
                placeholder={type === 'EXPENSE' ? 'Ej. Compra de papelería' : 'Ej. Lavado de vehículo'}
              />
              <TextField
                label="Valor"
                type="number"
                inputMode="numeric"
                min="1"
                adornment="$"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
              <SelectField label="Medio de pago" value={paymentMethodId} onChange={(e) => setPaymentMethodId(e.target.value)} required>
                {methods.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </SelectField>
              {error && <Alert>{error}</Alert>}
            </div>
            <div className="flex flex-col-reverse gap-2 border-t border-neutral-100 bg-neutral-50 px-6 py-4 sm:flex-row sm:justify-end">
              <Button variant="secondary" onClick={onClose} disabled={submitting}>
                Cancelar
              </Button>
              <Button type="submit" variant={type === 'EXPENSE' ? 'danger' : 'primary'} loading={submitting} disabled={!conceptId}>
                {type === 'EXPENSE' ? 'Registrar gasto' : 'Registrar ingreso'}
              </Button>
            </div>
          </form>
        )
      )}
    </dialog>
  )
}
