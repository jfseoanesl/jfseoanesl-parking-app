import type { ReactNode } from 'react'
import { Bike, Car, CarFront } from 'lucide-react'
import type { ParkingStatus } from '../api/types'

/** Icono por tipo de vehiculo (sin emojis: no-emoji-icons). */
export function VehicleIcon({ code, className = 'size-5' }: { code?: string | null; className?: string }) {
  if (code === 'MOTORCYCLE') return <Bike aria-hidden className={className} />
  if (code === 'CAR') return <Car aria-hidden className={className} />
  return <CarFront aria-hidden className={className} />
}

const PLATE_SIZES = {
  sm: 'px-2 py-0.5 text-base',
  md: 'px-3 py-1 text-xl',
  lg: 'px-4 py-1.5 text-plate',
}

/**
 * La placa como elemento visual principal (09-ux-specification.md §4 PlateBadge), con el
 * aspecto de una placa colombiana: fondo amarillo, borde y texto negros, tipografia mono.
 */
export function PlateBadge({ plate, size = 'md' }: { plate: string; size?: keyof typeof PLATE_SIZES }) {
  return (
    <span
      className={`inline-block rounded-md border-2 border-neutral-900 bg-plate-yellow font-mono font-bold tracking-wider text-neutral-900 shadow-[inset_0_0_0_2px_rgb(250_204_21)] ${PLATE_SIZES[size]}`}
    >
      {plate}
    </span>
  )
}

type BadgeTone = 'success' | 'neutral' | 'danger' | 'warning' | 'primary'

const BADGE_TONES: Record<BadgeTone, string> = {
  success: 'bg-success-50 text-success-700 ring-success-200',
  neutral: 'bg-neutral-100 text-neutral-700 ring-neutral-200',
  danger: 'bg-danger-50 text-danger-700 ring-danger-200',
  warning: 'bg-warning-50 text-warning-800 ring-warning-200',
  primary: 'bg-primary-50 text-primary-700 ring-primary-200',
}

/** Pastilla con punto de color + texto: el color nunca es el unico indicador. */
export function Badge({ tone, children, dot = true }: { tone: BadgeTone; children: ReactNode; dot?: boolean }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${BADGE_TONES[tone]}`}
    >
      {dot && <span aria-hidden className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}

/** Paleta semantica de estados de sesion (09-ux-specification.md §3.3). */
const STATUS: Record<ParkingStatus, { tone: BadgeTone; label: string }> = {
  EN_PARQUEADERO: { tone: 'success', label: 'En parqueadero' },
  FINALIZADO: { tone: 'neutral', label: 'Finalizado' },
  ANULADO: { tone: 'danger', label: 'Anulado' },
}

export function StatusBadge({ status }: { status: ParkingStatus }) {
  const { tone, label } = STATUS[status] ?? { tone: 'neutral', label: status }
  return <Badge tone={tone}>{label}</Badge>
}
