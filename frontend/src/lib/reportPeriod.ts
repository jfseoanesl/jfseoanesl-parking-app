import { localIsoDate } from './format'

/** Períodos de Reportes (RF-060 a RF-063, RF-078): compartidos por el reporte general y el de mensualidades. */
export type Preset = 'daily' | 'weekly' | 'monthly' | 'custom'

export const PRESETS: { value: Preset; label: string }[] = [
  { value: 'daily', label: 'Diario' },
  { value: 'weekly', label: 'Semanal' },
  { value: 'monthly', label: 'Mensual' },
  { value: 'custom', label: 'Rango' },
]

/** Semanal = últimos 7 días; mensual = últimos 30 (incluido hoy), en fecha local. */
export function rangeForPreset(
  preset: Preset,
  customFrom: string,
  customTo: string,
  today: Date = new Date(),
): { from: string; to: string } {
  if (preset === 'daily') return { from: localIsoDate(today), to: localIsoDate(today) }
  if (preset === 'weekly' || preset === 'monthly') {
    const from = new Date(today)
    from.setDate(from.getDate() - (preset === 'weekly' ? 6 : 29))
    return { from: localIsoDate(from), to: localIsoDate(today) }
  }
  return { from: customFrom, to: customTo }
}
