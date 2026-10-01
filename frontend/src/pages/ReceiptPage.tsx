import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Printer } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { ParkingConfiguration, Receipt } from '../api/types'
import { formatCurrency, formatDateTime, formatElapsed } from '../lib/format'
import { parseReceiptFormat } from '../lib/receiptFormat'
import { Button } from '../components/Button'
import { Alert, LoadingState } from '../components/Feedback'
import { PlateBadge } from '../components/Badges'

/**
 * RF-050 / BR-017: comprobante de pago imprimible (vista estándar del navegador: Imprimir o
 * "Guardar como PDF"). Los campos visibles siguen la configuración del comprobante (RF-015).
 */
export default function ReceiptPage() {
  const { paymentId } = useParams<{ paymentId: string }>()
  const navigate = useNavigate()
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
    <div className="mx-auto max-w-sm">
      <div className="mb-4 flex items-center justify-between gap-2 print:hidden">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="inline-flex min-h-11 items-center gap-2 rounded-lg text-sm font-medium text-neutral-600 transition-colors hover:text-neutral-900"
        >
          <ArrowLeft aria-hidden className="size-4" />
          Volver
        </button>
        <Button icon={<Printer aria-hidden className="size-5" />} onClick={() => window.print()}>
          Imprimir
        </Button>
      </div>

      <article
        aria-label="Comprobante de pago"
        className="card p-6 font-mono text-sm text-neutral-900 print:border-0 print:p-0 print:shadow-none"
      >
        <header className="border-b border-dashed border-neutral-300 pb-4 text-center">
          <h1 className="font-sans text-lg font-bold">{receipt.parkingName ?? 'Parqueadero'}</h1>
          {format.showNit && receipt.nit && <p>NIT {receipt.nit}</p>}
          {format.showAddress && receipt.address && <p>{receipt.address}</p>}
          {format.showPhone && receipt.phone && <p>Tel. {receipt.phone}</p>}
          <p className="mt-2 font-sans text-xs tracking-wider text-neutral-500 uppercase">Comprobante de pago</p>
        </header>

        <div className="flex justify-center py-4">
          <PlateBadge plate={receipt.plate} />
        </div>

        <dl className="space-y-1.5 border-b border-dashed border-neutral-300 pb-4">
          <Line label="Operación" value={receipt.operationNumber} />
          <Line label="Vehículo" value={receipt.vehicleTypeName} />
          <Line label="Ingreso" value={formatDateTime(receipt.entryDateTime)} />
          <Line label="Salida" value={formatDateTime(receipt.exitDateTime)} />
          {format.showDuration && <Line label="Permanencia" value={formatElapsed(receipt.elapsedMinutes)} />}
          {format.showTariff && <Line label="Tarifa" value={receipt.tariffName} />}
          <Line label="Medio de pago" value={receipt.paymentMethodName} />
          {format.showEmployee && <Line label="Atendió" value={receipt.employeeName} />}
        </dl>

        <p className="flex items-baseline justify-between pt-4 text-base font-bold">
          <span className="font-sans">Total</span>
          <span>{formatCurrency(receipt.chargedAmount)}</span>
        </p>
        {format.footer && (
          <p className="mt-4 border-t border-dashed border-neutral-300 pt-4 text-center font-sans text-xs text-neutral-600">
            {format.footer}
          </p>
        )}
      </article>
    </div>
  )
}

function Line({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="font-sans text-neutral-600">{label}</dt>
      <dd className="text-right">{value}</dd>
    </div>
  )
}
