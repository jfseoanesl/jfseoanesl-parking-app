import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { ParkingConfiguration, Receipt } from '../api/types'
import { formatCurrency, formatDateTime, formatElapsed } from '../lib/format'
import { parseReceiptFormat } from '../lib/receiptFormat'
import { Alert, LoadingState } from '../components/Feedback'
import { PlateBadge } from '../components/Badges'
import { ReceiptLayout, ReceiptLine } from '../components/ReceiptLayout'

/**
 * RF-050 / BR-017: comprobante de pago imprimible (vista estándar del navegador: Imprimir o
 * "Guardar como PDF"). Los campos visibles siguen la configuración del comprobante (RF-015).
 */
export default function ReceiptPage() {
  const { paymentId } = useParams<{ paymentId: string }>()
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const [config, setConfig] = useState<ParkingConfiguration | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!paymentId) return
    Promise.all([api.get<Receipt>(`/payments/${paymentId}/receipt`), api.get<ParkingConfiguration>('/configuration')])
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
    <ReceiptLayout title="Comprobante de pago" parking={receipt} format={format}>
      <div className="flex justify-center py-4">
        <PlateBadge plate={receipt.plate} />
      </div>

      <dl className="space-y-1.5 border-b border-dashed border-neutral-300 pb-4">
        <ReceiptLine label="Operación" value={receipt.operationNumber} />
        <ReceiptLine label="Vehículo" value={receipt.vehicleTypeName} />
        <ReceiptLine label="Ingreso" value={formatDateTime(receipt.entryDateTime)} />
        <ReceiptLine label="Salida" value={formatDateTime(receipt.exitDateTime)} />
        {format.showDuration && <ReceiptLine label="Permanencia" value={formatElapsed(receipt.elapsedMinutes)} />}
        {format.showTariff && <ReceiptLine label="Tarifa" value={receipt.tariffName} />}
        <ReceiptLine label="Medio de pago" value={receipt.paymentMethodName} />
        {format.showEmployee && <ReceiptLine label="Atendió" value={receipt.employeeName} />}
      </dl>

      <p className="flex items-baseline justify-between pt-4 text-base font-bold">
        <span className="font-sans">Total</span>
        <span>{formatCurrency(receipt.chargedAmount)}</span>
      </p>
    </ReceiptLayout>
  )
}
