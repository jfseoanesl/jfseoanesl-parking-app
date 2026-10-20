import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, Banknote, CalendarCheck, CalendarX, CircleCheck, Clock, FileSpreadsheet, FileText, Receipt, Search, Sheet, Users } from 'lucide-react'
import { api, ApiError } from '../../api/client'
import type { SubscriptionReport as Report, SubscriptionStatus } from '../../api/types'
import { formatCurrency, formatDate, formatDateTime, localIsoDate } from '../../lib/format'
import { PRESETS, rangeForPreset, type Preset } from '../../lib/reportPeriod'
import { SUBSCRIPTION_STATUS } from '../../lib/subscription'
import { saveBlob } from '../../lib/download'
import { Button } from '../Button'
import { Alert, EmptyState, Skeleton } from '../Feedback'
import { SegmentedControl, TextField } from '../Form'
import { Card, PageHeader, StatCard } from '../Layout'
import { Badge, PlateBadge } from '../Badges'
import { useToast } from '../Toast'

type ExportFormat = 'xlsx' | 'pdf' | 'csv'

const STATUS_ORDER: SubscriptionStatus[] = ['AL_DIA', 'EN_GRACIA', 'VENCIDA', 'SIN_PAGO', 'INACTIVA']

/**
 * RF-078: reporte de mensualidades. Período (pagos del rango), estados (multi-selección) y búsqueda
 * por cliente, documento o placa; el estado y los días de mora se calculan a hoy.
 */
export default function SubscriptionReport() {
  const toast = useToast()
  const navigate = useNavigate()
  const [preset, setPreset] = useState<Preset>('monthly')
  const [customFrom, setCustomFrom] = useState(localIsoDate())
  const [customTo, setCustomTo] = useState(localIsoDate())
  const [statuses, setStatuses] = useState<SubscriptionStatus[]>([])
  const [query, setQuery] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [report, setReport] = useState<Report | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState<ExportFormat | null>(null)

  const rangeInvalid = preset === 'custom' && (!customFrom || !customTo || customFrom > customTo)

  // La búsqueda se aplica medio segundo después de dejar de escribir.
  useEffect(() => {
    const id = setTimeout(() => setAppliedQuery(query.trim()), 400)
    return () => clearTimeout(id)
  }, [query])

  function params(): URLSearchParams {
    const { from, to } = rangeForPreset(preset, customFrom, customTo)
    const p = new URLSearchParams({ from, to })
    if (statuses.length > 0) p.set('status', statuses.join(','))
    if (appliedQuery) p.set('q', appliedQuery)
    return p
  }

  useEffect(() => {
    if (rangeInvalid) return
    setLoading(true)
    setError(null)
    api
      .get<Report>(`/reports/subscriptions?${params()}`)
      .then(setReport)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo generar el reporte'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset, customFrom, customTo, statuses, appliedQuery, rangeInvalid])

  async function handleExport(format: ExportFormat) {
    setExporting(format)
    try {
      const p = params()
      p.set('format', format)
      const { blob, filename } = await api.download(`/reports/subscriptions/export?${p}`, `reporte-mensualidades.${format}`)
      saveBlob(blob, filename)
      toast(`Reporte descargado: ${filename}`)
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo exportar el reporte', 'danger')
    } finally {
      setExporting(null)
    }
  }

  function toggleStatus(status: SubscriptionStatus) {
    setStatuses((current) => (current.includes(status) ? current.filter((s) => s !== status) : [...current, status]))
  }

  const periodLabel = report
    ? report.from === report.to
      ? formatDate(report.from)
      : `${formatDate(report.from)} — ${formatDate(report.to)}`
    : ''
  const exportDisabled = loading || !report || exporting !== null || rangeInvalid

  return (
    <div>
      <PageHeader
        title="Reporte de mensualidades"
        description={periodLabel ? `Pagos del ${periodLabel} · estado a hoy` : 'Cartera, pagos e indicadores'}
        actions={
          <>
            <Button variant="secondary" icon={<FileSpreadsheet aria-hidden className="size-5 text-success-700" />}
              loading={exporting === 'xlsx'} disabled={exportDisabled} onClick={() => handleExport('xlsx')}>
              Excel
            </Button>
            <Button variant="secondary" icon={<FileText aria-hidden className="size-5 text-danger-700" />}
              loading={exporting === 'pdf'} disabled={exportDisabled} onClick={() => handleExport('pdf')}>
              PDF
            </Button>
            <Button variant="secondary" icon={<Sheet aria-hidden className="size-5 text-neutral-600" />}
              loading={exporting === 'csv'} disabled={exportDisabled} onClick={() => handleExport('csv')}>
              CSV
            </Button>
          </>
        }
      />

      {/* Filtros en una fila, encima de los indicadores. */}
      <div className="mb-6 space-y-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end">
          <SegmentedControl label="Período" options={PRESETS} value={preset} onChange={setPreset} />
          {preset === 'custom' && (
            <div className="grid grid-cols-2 gap-3 lg:w-96">
              <TextField label="Desde" type="date" value={customFrom} max={customTo} onChange={(e) => setCustomFrom(e.target.value)} />
              <TextField label="Hasta" type="date" value={customTo} min={customFrom} onChange={(e) => setCustomTo(e.target.value)} />
            </div>
          )}
          <div className="relative flex-1">
            <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-neutral-400" />
            <input
              type="search"
              aria-label="Buscar en el reporte"
              className="input min-h-11 pl-11"
              placeholder="Cliente, documento o placa…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoComplete="off"
            />
          </div>
        </div>
        <div role="group" aria-label="Estados" className="flex flex-wrap items-center gap-2">
          <span className="text-sm text-neutral-600">Estado:</span>
          {STATUS_ORDER.map((status) => {
            const selected = statuses.includes(status)
            return (
              <button
                key={status}
                type="button"
                aria-pressed={selected}
                onClick={() => toggleStatus(status)}
                className={`min-h-9 rounded-full border px-3 text-sm font-medium transition-colors ${
                  selected ? 'border-primary-600 bg-primary-50 text-primary-700' : 'border-neutral-200 bg-white text-neutral-600 hover:bg-neutral-50'
                }`}
              >
                {SUBSCRIPTION_STATUS[status].label}
              </button>
            )
          })}
          {statuses.length > 0 && (
            <button type="button" onClick={() => setStatuses([])} className="min-h-9 px-2 text-sm font-medium text-primary-700 hover:underline">
              Todos
            </button>
          )}
        </div>
      </div>

      {rangeInvalid && <Alert tone="warning" className="mb-4">La fecha inicial debe ser anterior o igual a la final.</Alert>}
      {error && <Alert className="mb-4">{error}</Alert>}

      {loading || !report ? (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {Array.from({ length: 8 }, (_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatCard label={`Recaudo (${report.paymentsCount} pagos)`} value={formatCurrency(report.collected)}
              icon={<Banknote aria-hidden className="size-5" />} tone="success" />
            <StatCard label="Mensualidades activas" value={report.activeCount} icon={<Users aria-hidden className="size-5" />} />
            <StatCard label="Al día" value={report.countByStatus.AL_DIA} icon={<CircleCheck aria-hidden className="size-5" />} />
            <StatCard label="En gracia" value={report.countByStatus.EN_GRACIA} icon={<Clock aria-hidden className="size-5" />}
              tone={report.countByStatus.EN_GRACIA > 0 ? 'warning' : 'neutral'} />
            <StatCard label="Vencidas" value={report.countByStatus.VENCIDA} icon={<CalendarX aria-hidden className="size-5" />}
              tone={report.countByStatus.VENCIDA > 0 ? 'danger' : 'neutral'} />
            <StatCard label="Sin pago" value={report.countByStatus.SIN_PAGO} icon={<AlertTriangle aria-hidden className="size-5" />}
              tone={report.countByStatus.SIN_PAGO > 0 ? 'danger' : 'neutral'} />
            <StatCard label="Inactivas" value={report.countByStatus.INACTIVA} icon={<CalendarCheck aria-hidden className="size-5" />} tone="neutral" />
            <StatCard label="Cartera vencida estimada" value={formatCurrency(report.overdueAmount)}
              icon={<AlertTriangle aria-hidden className="size-5" />} tone={report.overdueAmount > 0 ? 'danger' : 'neutral'} />
          </div>

          <Card title={`Estado de cartera (${report.portfolio.length})`} description="Primero las que requieren gestión. Estado y mora a hoy." className="mb-6" bodyClassName="p-0">
            {report.portfolio.length === 0 ? (
              <div className="p-5">
                <EmptyState icon={<CalendarCheck aria-hidden className="size-6" />} title="Ninguna mensualidad coincide con los filtros" />
              </div>
            ) : (
              <>
                {/* Tabla en desktop */}
                <div className="hidden overflow-x-auto lg:block">
                  <table className="w-full text-sm">
                    <thead className="bg-neutral-50 text-left">
                      <tr>
                        {['Cliente', 'Placas', 'Valor', 'Corte', 'Pagada hasta', 'Estado', 'Mora', 'Pagado en el período'].map((h, i) => (
                          <th key={h} scope="col" className={`section-title px-4 py-3 ${i >= 2 && i !== 4 && i !== 5 ? 'text-right' : ''}`}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {report.portfolio.map((r) => (
                        <tr key={r.subscriptionId}>
                          <td className="px-4 py-3">
                            <p className="font-medium text-neutral-900">{r.customerName}</p>
                            {r.documentId && <p className="text-neutral-500">Doc. {r.documentId}</p>}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex flex-wrap gap-1">
                              {r.plates.map((p) => <PlateBadge key={p} plate={p} size="sm" />)}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(r.monthlyFee)}</td>
                          <td className="px-4 py-3 text-right tabular-nums">{r.cutOffDay}</td>
                          <td className="px-4 py-3 tabular-nums">{r.paidUntil ? formatDate(r.paidUntil) : '—'}</td>
                          <td className="px-4 py-3"><Badge tone={SUBSCRIPTION_STATUS[r.status].tone}>{SUBSCRIPTION_STATUS[r.status].label}</Badge></td>
                          <td className={`px-4 py-3 text-right tabular-nums ${r.daysOverdue ? 'font-semibold text-danger-700' : 'text-neutral-400'}`}>
                            {r.daysOverdue != null ? `${r.daysOverdue} d` : '—'}
                          </td>
                          <td className="px-4 py-3 text-right tabular-nums">
                            {formatCurrency(r.paidInPeriod)}
                            {r.paymentsInPeriod > 0 && <span className="text-neutral-500"> ({r.paymentsInPeriod})</span>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {/* Tarjetas en móvil */}
                <ul className="divide-y divide-neutral-100 lg:hidden">
                  {report.portfolio.map((r) => (
                    <li key={r.subscriptionId} className="space-y-2 px-4 py-3 text-sm">
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-medium text-neutral-900">{r.customerName}</p>
                        <Badge tone={SUBSCRIPTION_STATUS[r.status].tone}>{SUBSCRIPTION_STATUS[r.status].label}</Badge>
                      </div>
                      <div className="flex flex-wrap gap-1">{r.plates.map((p) => <PlateBadge key={p} plate={p} size="sm" />)}</div>
                      <p className="text-neutral-600">
                        {formatCurrency(r.monthlyFee)} · corte {r.cutOffDay} · {r.paidUntil ? `hasta ${formatDate(r.paidUntil)}` : 'sin pagos'}
                        {r.daysOverdue != null && <span className="font-semibold text-danger-700"> · {r.daysOverdue} días de mora</span>}
                      </p>
                      <p className="text-neutral-600">Pagado en el período: {formatCurrency(r.paidInPeriod)}</p>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Card>

          <Card title={`Detalle de pagos (${report.payments.length})`} description="Cobros de mensualidad en el período." bodyClassName="p-0">
            {report.payments.length === 0 ? (
              <p className="p-5 text-sm text-neutral-500">No hay pagos de mensualidad en el período con estos filtros.</p>
            ) : (
              <>
                <ul className="divide-y divide-neutral-100">
                  {report.payments.map((p) => (
                    <li key={p.paymentId} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-sm">
                      <span className="font-mono text-neutral-700">{p.operationNumber}</span>
                      <span className="min-w-40 flex-1">
                        <span className="font-medium text-neutral-900">{p.customerName}</span>
                        <span className="block text-neutral-500">
                          {formatDateTime(p.paidAt)} · cubre {formatDate(p.periodFrom)} – {formatDate(p.periodTo)}
                        </span>
                      </span>
                      <span className="text-neutral-600">{p.paymentMethodName} · {p.employeeName}</span>
                      <span className="w-24 text-right font-semibold tabular-nums">{formatCurrency(p.amount)}</span>
                      <Button variant="ghost" aria-label={`Comprobante ${p.operationNumber}`} icon={<Receipt aria-hidden className="size-4" />}
                        onClick={() => navigate(`/comprobantes-mensualidad/${p.paymentId}`)} />
                    </li>
                  ))}
                </ul>
                <p className="flex justify-between border-t border-neutral-200 px-4 py-3 text-sm font-semibold">
                  <span>Total</span>
                  <span className="tabular-nums">{formatCurrency(report.collected)}</span>
                </p>
              </>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
