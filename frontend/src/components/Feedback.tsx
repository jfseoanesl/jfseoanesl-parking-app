import type { ReactNode } from 'react'
import { CircleAlert, CircleCheck, Info, LoaderCircle, TriangleAlert } from 'lucide-react'

type Tone = 'danger' | 'success' | 'warning' | 'info'

const ALERT_TONES: Record<Tone, { box: string; icon: ReactNode }> = {
  danger: {
    box: 'border-danger-200 bg-danger-50 text-danger-800',
    icon: <CircleAlert aria-hidden className="size-5 shrink-0 text-danger-600" />,
  },
  success: {
    box: 'border-success-200 bg-success-50 text-success-800',
    icon: <CircleCheck aria-hidden className="size-5 shrink-0 text-success-600" />,
  },
  warning: {
    box: 'border-warning-200 bg-warning-50 text-warning-800',
    icon: <TriangleAlert aria-hidden className="size-5 shrink-0 text-warning-600" />,
  },
  info: {
    box: 'border-primary-200 bg-primary-50 text-primary-800',
    icon: <Info aria-hidden className="size-5 shrink-0 text-primary-600" />,
  },
}

/** Mensaje en linea junto al problema (error-feedback). Los errores se anuncian (role=alert). */
export function Alert({ tone = 'danger', children, className = '' }: { tone?: Tone; children: ReactNode; className?: string }) {
  const { box, icon } = ALERT_TONES[tone]
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={`flex items-start gap-2 rounded-lg border px-3 py-2.5 text-sm ${box} ${className}`}
    >
      {icon}
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}

/** Estado de carga con spinner (loading-states): nunca una pantalla congelada. */
export function LoadingState({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-2 py-12 text-neutral-500">
      <LoaderCircle aria-hidden className="size-5 animate-spin" />
      <span>{label}</span>
    </div>
  )
}

/** Bloques grises con la forma del contenido: reserva espacio y evita saltos (content-jumping). */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden className={`animate-pulse rounded-lg bg-neutral-200/70 ${className}`} />
}

/** Lista vacia (09-ux-specification.md §4 EmptyState). */
export function EmptyState({ icon, title, description, action }: {
  icon: ReactNode
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="card flex flex-col items-center px-6 py-12 text-center">
      <div className="mb-3 flex size-12 items-center justify-center rounded-full bg-neutral-100 text-neutral-500">
        {icon}
      </div>
      <p className="font-medium text-neutral-900">{title}</p>
      {description && <p className="mt-1 max-w-sm text-sm text-neutral-600">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
