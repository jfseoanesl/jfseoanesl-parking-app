import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { ParkingConfiguration, SubscriptionReceipt } from '../api/types'
import { formatCurrency, formatDate, formatDateTime } from '../lib/format'
import { parseReceiptFormat } from '../lib/receiptFormat'
import { Alert, LoadingState } from '../components/Feedback'
import { PlateBadge } from '../components/Badges'
import { ReceiptLayout, ReceiptLine } from '../components/ReceiptLayout'

/** RF-077: comprobante imprimible del pago de una mensualidad. */
export default function SubscriptionReceiptPage() {
  const { paymentId } = useParams<{ paymentId: string }>()
  const [receipt, setReceipt] = useState<SubscriptionReceipt | null>(null)
  const [config, setConfig] = useState<ParkingConfiguration | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!paymentId) return
    Promise.all([
      api.get<SubscriptionReceipt>(`/subscription-payments/${paymentId}/receipt`),
      api.get<ParkingConfiguration>('/configuration'),
    ])
      .then(([r, c]) => {
        setReceipt(r)
        setConfig(c)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el comprobante'))
  }, [paymentId])

  if (error) return <Alert>{error}</Alert>
  if (!receipt) return <LoadingState />

  const format = parseReceiptFormat(config?.receiptFormatJson)

  return (
    <ReceiptLayout title="Comprobante de mensualidad" parking={receipt} format={format}>
      <div className="flex flex-wrap justify-center gap-2 py-4">
        {receipt.plates.map((plate) => (
          <PlateBadge key={plate} plate={plate} size="sm" />
        ))}
      </div>

      <dl className="space-y-1.5 border-b border-dashed border-neutral-300 pb-4">
        <ReceiptLine label="Operación" value={receipt.operationNumber} />
        <ReceiptLine label="Cliente" value={receipt.customerName} />
        {receipt.documentId && <ReceiptLine label="Documento" value={receipt.documentId} />}
        <ReceiptLine label="Periodo" value={`${formatDate(receipt.periodFrom)} – ${formatDate(receipt.periodTo)}`} />
        <ReceiptLine label="Fecha de pago" value={formatDateTime(receipt.paidAt)} />
        <ReceiptLine label="Medio de pago" value={receipt.paymentMethodName ?? '—'} />
        {format.showEmployee && receipt.employeeName && <ReceiptLine label="Atendió" value={receipt.employeeName} />}
      </dl>

      <p className="flex items-baseline justify-between pt-4 text-base font-bold">
        <span className="font-sans">Total</span>
        <span>{formatCurrency(receipt.amount)}</span>
      </p>
    </ReceiptLayout>
  )
}
