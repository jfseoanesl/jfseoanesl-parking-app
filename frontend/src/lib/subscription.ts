import type { SubscriptionStatus } from '../api/types'

/** BR-028: etiqueta y tono de cada estado de mensualidad. */
export const SUBSCRIPTION_STATUS: Record<SubscriptionStatus, { label: string; tone: 'success' | 'warning' | 'danger' | 'neutral' }> = {
  AL_DIA: { label: 'Al día', tone: 'success' },
  EN_GRACIA: { label: 'En gracia', tone: 'warning' },
  VENCIDA: { label: 'Vencida', tone: 'danger' },
  SIN_PAGO: { label: 'Sin pago', tone: 'danger' },
  INACTIVA: { label: 'Inactiva', tone: 'neutral' },
}

/** Búsqueda por cliente, documento o placa (sin distinguir mayúsculas ni espacios en la placa). */
export function matchesSubscription(
  s: { customerName: string; documentId: string | null; vehicles: { plate: string }[] },
  query: string,
): boolean {
  const q = query.trim().toLowerCase()
  if (!q) return true
  const plateQuery = q.toUpperCase().replace(/\s+/g, '')
  return (
    s.customerName.toLowerCase().includes(q) ||
    (s.documentId ?? '').toLowerCase().includes(q) ||
    s.vehicles.some((v) => v.plate.includes(plateQuery))
  )
}
