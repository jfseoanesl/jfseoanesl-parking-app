import { useEffect, useState } from 'react'
import { Ban, Banknote, CalendarCheck, CircleArrowDown, CircleCheck, Clock, FileSpreadsheet, FileText, Sheet, Scale, TrendingUp } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { Report } from '../api/types'
import { formatCurrency, formatDate, formatElapsed, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { SegmentedControl, TextField } from '../components/Form'
import { Card, PageHeader, StatCard } from '../components/Layout'
import { useToast } from '../components/Toast'
import { saveBlob } from '../lib/download'

type Preset = 'daily' | 'weekly' | 'monthly' | 'custom'
type ExportFormat = 'xlsx' | 'pdf' | 'csv'


const PRESETS: { value: Preset; label: string }[] = [
  { value: 'daily', label: 'Diario' },
  { value: 'weekly', label: 'Semanal' },
  { value: 'monthly', label: 'Mensual' },
  { value: 'custom', label: 'Rango' },
]

function isoDate(d: Date): string {
  return localIsoDate(d)
}

function rangeForPreset(preset: Preset, customFrom: string, customTo: string): { from: string; to: string } {
  const today = new Date()
  if (preset === 'daily') return { from: isoDate(today), to: isoDate(today) }
  if (preset === 'weekly') {
    const from = new Date(today)
    from.setDate(from.getDate() - 6)
    return { from: isoDate(from), to: isoDate(today) }
  }
  if (preset === 'monthly') {
    const from = new Date(today)
    from.setDate(from.getDate() - 29)
    return { from: isoDate(from), to: isoDate(today) }
  }
  return { from: customFrom, to: customTo }
}

/** UC-010 (RF-060 a RF-063). Wireframe: 09-ux-specification.md §5.8. */
export default function ReportsPage() {
  const [preset, setPreset] = useState<Preset>('daily')
  const [customFrom, setCustomFrom] = useState(isoDate(new Date()))
  const [customTo, setCustomTo] = useState(isoDate(new Date()))
  const [report, setReport] = useState<Report | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState<ExportFormat | null>(null)
  const toast = useToast()

  /** RF-063: exporta exactamente el periodo que se esta viendo. */
  async function handleExport(format: ExportFormat) {
    const { from, to } = rangeForPreset(preset, customFrom, customTo)
    setExporting(format)
    try {
      const { blob, filename } = await api.download(
        `/reports/summary/export?from=${from}&to=${to}&format=${format}`,
        `reporte-parqueadero.${format}`,
      )
      saveBlob(blob, filename)
      toast(`Reporte descargado: ${filename}`)
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo exportar el reporte', 'danger')
    } finally {
      setExporting(null)
    }
  }

  const rangeInvalid = preset === 'custom' && (!customFrom || !customTo || customFrom > customTo)

  useEffect(() => {
    const { from, to } = rangeForPreset(preset, customFrom, customTo)
    if (!from || !to) return
    setLoading(true)
    setError(null)
    api
      .get<Report>(`/reports/summary?from=${from}&to=${to}`)
      .then(setReport)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo generar el reporte'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset, customFrom, customTo])

  const periodLabel = report
    ? report.from === report.to
      ? formatDate(report.from)
      : `${formatDate(report.from)} — ${formatDate(report.to)}`
    : ''

  return (
    <div>
      <PageHeader
        title="Reportes"
        description={periodLabel || 'Indicadores del período seleccionado'}
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileSpreadsheet aria-hidden className="size-5 text-success-700" />}
              loading={exporting === 'xlsx'}
              disabled={loading || !report || exporting !== null || rangeInvalid}
              onClick={() => handleExport('xlsx')}
            >
              Excel
            </Button>
            <Button
              variant="secondary"
              icon={<FileText aria-hidden className="size-5 text-danger-700" />}
              loading={exporting === 'pdf'}
              disabled={loading || !report || exporting !== null || rangeInvalid}
              onClick={() => handleExport('pdf')}
            >
              PDF
            </Button>
            <Button
              variant="secondary"
              icon={<Sheet aria-hidden className="size-5 text-neutral-600" />}
              loading={exporting === 'csv'}
              disabled={loading || !report || exporting !== null || rangeInvalid}
              onClick={() => handleExport('csv')}
            >
              CSV
            </Button>
          </>
        }
      />

      {/* Filtros en una sola fila, encima de los indicadores (dataviz: interaction). */}
      <div className="mb-6 flex flex-col gap-3 lg:flex-row lg:items-end">
        <SegmentedControl label="Período" options={PRESETS} value={preset} onChange={setPreset} />
        {preset === 'custom' && (
          <div className="grid grid-cols-2 gap-3 lg:w-96">
            <TextField label="Desde" type="date" value={customFrom} max={customTo} onChange={(e) => setCustomFrom(e.target.value)} />
            <TextField label="Hasta" type="date" value={customTo} min={customFrom} onChange={(e) => setCustomTo(e.target.value)} />
          </div>
        )}
      </div>

      {error && <Alert className="mb-4">{error}</Alert>}

      {loading ? (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {Array.from({ length: 8 }, (_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : (
        report && (
          <>
            <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
              <StatCard label="Total recaudado" value={formatCurrency(report.totalCollected)} icon={<Banknote aria-hidden className="size-5" />} tone="success" />
              <StatCard label="Vehículos ingresados" value={report.vehiclesEntered} icon={<CircleArrowDown aria-hidden className="size-5" />} />
              <StatCard label="Vehículos finalizados" value={report.vehiclesFinalized} icon={<CircleCheck aria-hidden className="size-5" />} />
              <StatCard label="Parqueos anulados" value={report.cancelledSessions} icon={<Ban aria-hidden className="size-5" />} tone={report.cancelledSessions > 0 ? 'danger' : 'neutral'} />
              <StatCard
                label="Duración promedio"
                value={report.averageDurationMinutes != null ? formatElapsed(Math.round(report.averageDurationMinutes)) : '—'}
                icon={<Clock aria-hidden className="size-5" />}
                tone="neutral"
              />
              <StatCard
                label="Hora de mayor movimiento"
                value={report.peakHour != null ? `${report.peakHour}:00` : '—'}
                icon={<TrendingUp aria-hidden className="size-5" />}
                tone="neutral"
              />
              <StatCard
                label="Diferencias de caja"
                value={formatCurrency(report.cashDifferencesSum)}
                icon={<Scale aria-hidden className="size-5" />}
                tone={report.cashDifferencesSum === 0 ? 'neutral' : report.cashDifferencesSum < 0 ? 'danger' : 'warning'}
              />
              {/* BR-031 / BR-030: incluidas en el total recaudado. */}
              <StatCard
                label={`Mensualidades (${report.subscriptionPayments})`}
                value={formatCurrency(report.subscriptionCollected)}
                icon={<CalendarCheck aria-hidden className="size-5" />}
                tone="neutral"
              />
              <StatCard
                label="Salidas de clientes mensuales"
                value={report.subscriberExits}
                icon={<CalendarCheck aria-hidden className="size-5" />}
                tone="neutral"
              />
            </div>

            {report.vehiclesEntered === 0 && report.totalCollected === 0 ? (
              <EmptyState
                icon={<TrendingUp aria-hidden className="size-6" />}
                title="Sin actividad en este período"
                description="Elige otro período para ver los desgloses."
              />
            ) : (
              <div className="grid gap-4 lg:grid-cols-2">
                <Breakdown title="Vehículos por tipo" entries={report.enteredByVehicleType} isCount />
                <Breakdown title="Recaudo por medio de pago" entries={report.collectedByPaymentMethod} />
                <Breakdown title="Recaudo por tarifa" entries={report.collectedByTariff} />
                <Breakdown title="Recaudo por empleado" entries={report.collectedByEmployee} />
              </div>
            )}
          </>
        )
      )}
    </div>
  )
}

/**
 * Desglose como lista de barras horizontales (dataviz: comparar magnitud -> barra). Una sola
 * serie -> un solo color para todas las barras; nombre y valor en tinta de texto, nunca en el
 * color de la barra. La lista misma es la vista tabular (nombre + valor siempre visibles).
 */
function Breakdown({ title, entries, isCount = false }: { title: string; entries: Record<string, number>; isCount?: boolean }) {
  const rows = Object.entries(entries).sort((a, b) => b[1] - a[1])
  if (rows.length === 0) return null
  const max = Math.max(...rows.map(([, v]) => v), 1)
  const total = rows.reduce((sum, [, v]) => sum + v, 0)

  return (
    <Card title={title}>
      <ul className="space-y-4">
        {rows.map(([key, value]) => {
          const share = total > 0 ? Math.round((value / total) * 100) : 0
          return (
            <li key={key} title={`${key}: ${isCount ? value : formatCurrency(value)} (${share}% del total)`}>
              <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
                <span className="truncate text-neutral-700">{key}</span>
                <span className="shrink-0 font-medium tabular-nums text-neutral-900">
                  {isCount ? value : formatCurrency(value)}
                  <span className="ml-1.5 text-xs font-normal text-neutral-500">{share}%</span>
                </span>
              </div>
              <div className="h-2 w-full rounded-full bg-primary-50" aria-hidden>
                <div
                  className="h-full rounded-full bg-primary-600 transition-[width] duration-300"
                  style={{ width: value === 0 ? 0 : `${Math.max(2, (value / max) * 100)}%` }}
                />
              </div>
            </li>
          )
        })}
      </ul>
    </Card>
  )
}
