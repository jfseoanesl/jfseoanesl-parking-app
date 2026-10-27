import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { CashTransactionReceipt, ParkingConfiguration } from '../api/types'
import { formatCurrency, formatDateTime } from '../lib/format'
import { parseReceiptFormat } from '../lib/receiptFormat'
import { Alert, LoadingState } from '../components/Feedback'
import { ReceiptLayout, ReceiptLine } from '../components/ReceiptLayout'

/** RF-082: comprobante imprimible de otro ingreso o de un gasto de caja. */
export default function CashTransactionReceiptPage() {
  const { transactionId } = useParams<{ transactionId: string }>()
  const [receipt, setReceipt] = useState<CashTransactionReceipt | null>(null)
  const [config, setConfig] = useState<ParkingConfiguration | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!transactionId) return
    Promise.all([
      api.get<CashTransactionReceipt>(`/cash-transactions/${transactionId}/receipt`),
      api.get<ParkingConfiguration>('/configuration'),
    ])
      .then(([r, c]) => {
        setReceipt(r)
        setConfig(c)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el comprobante'))
  }, [transactionId])

  if (error) return <Alert>{error}</Alert>
  if (!receipt) return <LoadingState />

  const format = parseReceiptFormat(config?.receiptFormatJson)
  const t = receipt.transaction
  const isExpense = t.type === 'EXPENSE'
  const voided = t.status === 'VOIDED'

  return (
    <ReceiptLayout
      title={isExpense ? 'Comprobante de gasto' : 'Comprobante de ingreso'}
      parking={receipt}
      format={format}
      notice={voided ? `ANULADO${t.voidReason ? ` — ${t.voidReason}` : ''}` : undefined}
    >
      <dl className="space-y-1.5 border-b border-dashed border-neutral-300 py-4">
        <ReceiptLine label="Operación" value={t.operationNumber} />
        <ReceiptLine label="Concepto" value={t.conceptName} />
        {t.description && <ReceiptLine label="Descripción" value={t.description} />}
        <ReceiptLine label="Fecha" value={formatDateTime(t.createdAt)} />
        <ReceiptLine label="Medio de pago" value={t.paymentMethodName ?? '—'} />
        {isExpense && t.cashierName && <ReceiptLine label="Caja de" value={t.cashierName} />}
        {format.showEmployee && t.userName && <ReceiptLine label="Registró" value={t.userName} />}
      </dl>

      <p className="flex items-baseline justify-between pt-4 text-base font-bold">
        <span className="font-sans">{isExpense ? 'Valor del gasto' : 'Total'}</span>
        <span className={voided ? 'line-through' : ''}>{formatCurrency(t.amount)}</span>
      </p>
    </ReceiptLayout>
  )
}
