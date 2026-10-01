import type { ReactNode } from 'react'

/** Cabecera de pagina uniforme: titulo + descripcion opcional + acciones a la derecha. */
export function PageHeader({ title, description, actions }: {
  title: string
  description?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        <h1 className="text-2xl font-semibold tracking-tight text-neutral-900">{title}</h1>
        {description && <p className="mt-1 text-sm text-neutral-600">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

/** Tarjeta de seccion con titulo opcional. */
export function Card({ title, description, actions, children, className = '', bodyClassName = 'p-5' }: {
  title?: string
  description?: string
  actions?: ReactNode
  children: ReactNode
  className?: string
  bodyClassName?: string
}) {
  return (
    <section className={`card ${className}`}>
      {title && (
        <header className="flex flex-wrap items-center justify-between gap-2 border-b border-neutral-100 px-5 py-4">
          <div>
            <h2 className="font-semibold text-neutral-900">{title}</h2>
            {description && <p className="mt-0.5 text-sm text-neutral-600">{description}</p>}
          </div>
          {actions}
        </header>
      )}
      <div className={bodyClassName}>{children}</div>
    </section>
  )
}

type StatTone = 'primary' | 'success' | 'warning' | 'danger' | 'neutral'

const STAT_ICON_TONES: Record<StatTone, string> = {
  primary: 'bg-primary-50 text-primary-600',
  success: 'bg-success-50 text-success-600',
  warning: 'bg-warning-50 text-warning-600',
  danger: 'bg-danger-50 text-danger-600',
  neutral: 'bg-neutral-100 text-neutral-600',
}

/** Numero grande + etiqueta (09-ux-specification.md §4 StatCard). */
export function StatCard({ label, value, icon, tone = 'primary', hint }: {
  label: string
  value: ReactNode
  icon?: ReactNode
  tone?: StatTone
  hint?: ReactNode
}) {
  return (
    <div className="card p-4">
      {/* Icono + etiqueta arriba y valor debajo a todo el ancho: un monto largo nunca se recorta
          en la grilla de 2 columnas del movil (dataviz: nada de etiquetas truncadas). */}
      <div className="flex items-center gap-2.5">
        {icon && (
          <div className={`flex size-9 shrink-0 items-center justify-center rounded-lg ${STAT_ICON_TONES[tone]}`}>
            {icon}
          </div>
        )}
        <p className="min-w-0 text-sm leading-tight text-neutral-600">{label}</p>
      </div>
      <p className="mt-3 text-xl font-semibold break-words text-neutral-900 sm:text-2xl">{value}</p>
      {hint && <p className="mt-0.5 text-xs text-neutral-500">{hint}</p>}
    </div>
  )
}

/** Fila etiqueta/valor para resumenes (caja, comprobante). */
export function SummaryRow({ label, value, emphasis = false, valueClassName = '' }: {
  label: ReactNode
  value: ReactNode
  emphasis?: boolean
  valueClassName?: string
}) {
  return (
    <div className={`flex items-baseline justify-between gap-4 py-2 ${emphasis ? 'text-base' : 'text-sm'}`}>
      <dt className={emphasis ? 'font-medium text-neutral-900' : 'text-neutral-600'}>{label}</dt>
      <dd className={`tabular-nums ${emphasis ? 'font-semibold text-neutral-900' : 'text-neutral-900'} ${valueClassName}`}>
        {value}
      </dd>
    </div>
  )
}
