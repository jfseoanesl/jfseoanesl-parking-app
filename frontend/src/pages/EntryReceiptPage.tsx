import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import type { EntryReceipt, ParkingConfiguration } from '../api/types'
import { formatDateTime } from '../lib/format'
import { parseReceiptFormat } from '../lib/receiptFormat'
import { Alert, LoadingState } from '../components/Feedback'
import { PlateBadge } from '../components/Badges'
import { ReceiptLayout, ReceiptLine } from '../components/ReceiptLayout'

/**
 * RF-072: comprobante de ingreso, soporte para el cliente de que su vehículo ingresó. Lleva el
 * consecutivo ING (BR-027), observaciones del ingreso y, según el formato (RF-015), el empleado.
 */
export default function EntryReceiptPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const [receipt, setReceipt] = useState<EntryReceipt | null>(null)
  const [config, setConfig] = useState<ParkingConfiguration | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!sessionId) return
    Promise.all([
      api.get<EntryReceipt>(`/parking-sessions/${sessionId}/entry-receipt`),
      api.get<ParkingConfiguration>('/configuration'),
    ])
      .then(([r, c]) => {
        setReceipt(r)
        setConfig(c)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el comprobante'))
  }, [sessionId])

  if (error) return <Alert>{error}</Alert>
  if (!receipt) return <LoadingState />

  const format = parseReceiptFormat(config?.receiptFormatJson)

  return (
    <ReceiptLayout
      title="Comprobante de ingreso"
      parking={receipt}
      format={format}
      notice="Conserve este comprobante para retirar su vehículo."
    >
      <div className="flex justify-center py-4">
        <PlateBadge plate={receipt.plate} />
      </div>

      <dl className="space-y-1.5">
        <ReceiptLine label="Ingreso N.º" value={<span className="font-bold">{receipt.entryNumber ?? '—'}</span>} />
        <ReceiptLine label="Vehículo" value={receipt.vehicleTypeName ?? '—'} />
        <ReceiptLine label="Fecha y hora" value={formatDateTime(receipt.entryDateTime)} />
        {format.showEmployee && receipt.employeeName && <ReceiptLine label="Registró" value={receipt.employeeName} />}
      </dl>

      {receipt.notes && (
        <div className="mt-4 border-t border-dashed border-neutral-300 pt-4">
          <p className="font-sans text-neutral-600">Observaciones</p>
          <p className="mt-1 whitespace-pre-wrap">{receipt.notes}</p>
        </div>
      )}
    </ReceiptLayout>
  )
}
