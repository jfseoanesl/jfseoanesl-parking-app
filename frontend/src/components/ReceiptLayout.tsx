import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Printer } from 'lucide-react'
import type { ReceiptFormat } from '../api/types'
import { Button } from './Button'

/**
 * Marco común de los comprobantes imprimibles (pago RF-050, ingreso RF-072): barra Volver/Imprimir
 * (oculta al imprimir), encabezado con los datos del parqueadero según el formato (RF-015) y pie.
 */
export function ReceiptLayout({
  title,
  parking,
  format,
  children,
  notice,
}: {
  title: string
  parking: { parkingName: string | null; nit: string | null; address: string | null; phone: string | null }
  format: ReceiptFormat
  children: ReactNode
  /** Texto fijo antes del pie configurable (ej. instrucciones para el cliente). */
  notice?: string
}) {
  const navigate = useNavigate()

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
        aria-label={title}
        className="card p-6 font-mono text-sm text-neutral-900 print:border-0 print:p-0 print:shadow-none"
      >
        <header className="border-b border-dashed border-neutral-300 pb-4 text-center">
          <h1 className="font-sans text-lg font-bold">{parking.parkingName ?? 'Parqueadero'}</h1>
          {format.showNit && parking.nit && <p>NIT {parking.nit}</p>}
          {format.showAddress && parking.address && <p>{parking.address}</p>}
          {format.showPhone && parking.phone && <p>Tel. {parking.phone}</p>}
          <p className="mt-2 font-sans text-xs tracking-wider text-neutral-500 uppercase">{title}</p>
        </header>

        {children}

        {(notice || format.footer) && (
          <div className="mt-4 space-y-2 border-t border-dashed border-neutral-300 pt-4 text-center font-sans text-xs text-neutral-600">
            {notice && <p className="font-medium text-neutral-800">{notice}</p>}
            {format.footer && <p>{format.footer}</p>}
          </div>
        )}
      </article>
    </div>
  )
}

export function ReceiptLine({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="font-sans text-neutral-600">{label}</dt>
      <dd className="text-right">{value}</dd>
    </div>
  )
}
