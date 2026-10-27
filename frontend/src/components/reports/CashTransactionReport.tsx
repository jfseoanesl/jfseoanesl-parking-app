import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Ban, CircleMinus, FileSpreadsheet, FileText, HandCoins, Landmark, Receipt, Sheet, Wallet } from 'lucide-react'
import { api, ApiError } from '../../api/client'
import type { CashConcept, CashTransactionReport as Report, CashTransactionType } from '../../api/types'
import { formatCurrency, formatDate, formatDateTime, localIsoDate } from '../../lib/format'
import { PRESETS, rangeForPreset, type Preset } from '../../lib/reportPeriod'
import { saveBlob } from '../../lib/download'
import { Button } from '../Button'
import { Alert, EmptyState, Skeleton } from '../Feedback'
import { SegmentedControl, SelectField, TextField } from '../Form'
import { Card, PageHeader, StatCard } from '../Layout'
import { Badge } from '../Badges'
import { useToast } from '../Toast'

type ExportFormat = 'xlsx' | 'pdf' | 'csv'
type TypeFilter = 'ALL' | CashTransactionType
type StatusFilter = 'ALL' | 'ACTIVE' | 'VOIDED'

const TYPE_OPTIONS: { value: TypeFilter; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'INCOME', label: 'Ingresos' },
  { value: 'EXPENSE', label: 'Gastos' },
]

const TYPE_LABEL: Record<CashTransactionType, string> = { INCOME: 'Ingreso', EXPENSE: 'Gasto' }

/** RF-083: otros ingresos y gastos de caja por período, tipo, concepto y estado. */
export default function CashTransactionReport() {
  const toast = useToast()
  const navigate = useNavigate()
  const [preset, setPreset] = useState<Preset>('monthly')
  const [customFrom, setCustomFrom] = useState(localIsoDate())
  const [customTo, setCustomTo] = useState(localIsoDate())
  const [type, setType] = useState<TypeFilter>('ALL')
  const [conceptId, setConceptId] = useState('')
  const [status, setStatus] = useState<StatusFilter>('ALL')
  const [concepts, setConcepts] = useState<CashConcept[]>([])
  const [report, setReport] = useState<Report | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState<ExportFormat | null>(null)

  const rangeInvalid = preset === 'custom' && (!customFrom || !customTo || customFrom > customTo)

  useEffect(() => {
    api.get<CashConcept[]>('/cash-concepts').then(setConcepts).catch(() => setConcepts([]))
  }, [])

  function params(): URLSearchParams {
    const { from, to } = rangeForPreset(preset, customFrom, customTo)
    const p = new URLSearchParams({ from, to })
    if (type !== 'ALL') p.set('type', type)
    if (conceptId) p.set('conceptId', conceptId)
    if (status !== 'ALL') p.set('status', status)
    return p
  }

  useEffect(() => {
    if (rangeInvalid) return
    setLoading(true)
    setError(null)
    api
      .get<Report>(`/reports/cash-transactions?${params()}`)
      .then(setReport)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo generar el reporte'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset, customFrom, customTo, type, conceptId, status, rangeInvalid])

  async function handleExport(format: ExportFormat) {
    setExporting(format)
    try {
      const p = params()
      p.set('format', format)
      const { blob, filename } = await api.download(`/reports/cash-transactions/export?${p}`, `reporte-ingresos-gastos.${format}`)
      saveBlob(blob, filename)
      toast(`Reporte descargado: ${filename}`)
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo exportar el reporte', 'danger')
    } finally {
      setExporting(null)
    }
  }

  const visibleConcepts = concepts.filter((c) => type === 'ALL' || c.type === type)
  const periodLabel = report
    ? report.from === report.to
      ? formatDate(report.from)
      : `${formatDate(report.from)} — ${formatDate(report.to)}`
    : ''
  const exportDisabled = loading || !report || exporting !== null || rangeInvalid

  return (
    <div>
      <PageHeader
        title="Ingresos y gastos de caja"
        description={periodLabel ? `Movimientos del ${periodLabel}` : 'Otros ingresos y gastos registrados en caja'}
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

      <div className="mb-6 space-y-3">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end">
          <SegmentedControl label="Período" options={PRESETS} value={preset} onChange={setPreset} />
          {preset === 'custom' && (
            <div className="grid grid-cols-2 gap-3 lg:w-96">
              <TextField label="Desde" type="date" value={customFrom} max={customTo} onChange={(e) => setCustomFrom(e.target.value)} />
              <TextField label="Hasta" type="date" value={customTo} min={customFrom} onChange={(e) => setCustomTo(e.target.value)} />
            </div>
          )}
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          <SelectField
            label="Tipo"
            value={type}
            onChange={(e) => {
              setType(e.target.value as TypeFilter)
              setConceptId('')
            }}
          >
            {TYPE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </SelectField>
          <SelectField label="Concepto" value={conceptId} onChange={(e) => setConceptId(e.target.value)}>
            <option value="">Todos</option>
            {visibleConcepts.map((c) => (
              <option key={c.id} value={c.id}>
                {type === 'ALL' ? `${TYPE_LABEL[c.type]} · ` : ''}
                {c.name}
              </option>
            ))}
          </SelectField>
          <SelectField label="Estado" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
            <option value="ALL">Todos</option>
            <option value="ACTIVE">Activos</option>
            <option value="VOIDED">Anulados</option>
          </SelectField>
        </div>
      </div>

      {rangeInvalid && <Alert tone="warning" className="mb-4">La fecha inicial debe ser anterior o igual a la final.</Alert>}
      {error && <Alert className="mb-4">{error}</Alert>}

      {loading || !report ? (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
            <StatCard label={`Otros ingresos (${report.incomeCount})`} value={formatCurrency(report.incomeTotal)}
              icon={<HandCoins aria-hidden className="size-5" />} tone="success" />
            <StatCard label={`Gastos de caja (${report.expensesCount})`} value={formatCurrency(report.expensesTotal)}
              icon={<CircleMinus aria-hidden className="size-5" />} tone={report.expensesTotal > 0 ? 'warning' : 'neutral'} />
            <StatCard label="Neto (ingresos − gastos)" value={formatCurrency(report.net)} icon={<Landmark aria-hidden className="size-5" />}
              tone={report.net < 0 ? 'danger' : 'primary'} />
            <StatCard label="Anulados" value={report.voidedCount} icon={<Ban aria-hidden className="size-5" />} tone="neutral" />
          </div>

          <Card title="Totales por concepto" description="Solo movimientos activos." className="mb-6" bodyClassName="p-0">
            {report.byConcept.length === 0 ? (
              <p className="p-5 text-sm text-neutral-500">Sin movimientos activos en el período.</p>
            ) : (
              <ul className="divide-y divide-neutral-100">
                {report.byConcept.map((r) => (
                  <li key={`${r.type}-${r.conceptName}`} className="flex items-center gap-3 px-4 py-3 text-sm">
                    <Badge tone={r.type === 'EXPENSE' ? 'warning' : 'success'}>{TYPE_LABEL[r.type]}</Badge>
                    <span className="min-w-0 flex-1 text-neutral-900">{r.conceptName}</span>
                    <span className="text-neutral-500">{r.count} mov.</span>
                    <span className={`w-28 text-right font-semibold tabular-nums ${r.type === 'EXPENSE' ? 'text-danger-700' : ''}`}>
                      {r.type === 'EXPENSE' ? '− ' : ''}
                      {formatCurrency(r.total)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card title={`Detalle de movimientos (${report.transactions.length})`} bodyClassName="p-0">
            {report.transactions.length === 0 ? (
              <div className="p-5">
                <EmptyState icon={<Wallet aria-hidden className="size-6" />} title="Ningún movimiento coincide con los filtros" />
              </div>
            ) : (
              <ul className="divide-y divide-neutral-100">
                {report.transactions.map((t) => {
                  const voided = t.status === 'VOIDED'
                  return (
                    <li key={t.id} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-sm">
                      <span className="font-mono text-neutral-700">{t.operationNumber}</span>
                      <Badge tone={t.type === 'EXPENSE' ? 'warning' : 'success'}>{TYPE_LABEL[t.type]}</Badge>
                      {voided && <Badge tone="neutral">Anulado</Badge>}
                      <span className="min-w-40 flex-1">
                        <span className="font-medium text-neutral-900">
                          {t.conceptName}
                          {t.description && <span className="font-normal text-neutral-600"> · {t.description}</span>}
                        </span>
                        <span className="block text-neutral-500">
                          {formatDateTime(t.createdAt)} · {t.paymentMethodName ?? '—'} · registró {t.userName ?? '—'}
                          {t.cashierName && t.cashierName !== t.userName && <> · caja de {t.cashierName}</>}
                        </span>
                        {voided && t.voidReason && <span className="block text-neutral-500">Motivo: {t.voidReason}</span>}
                      </span>
                      <span className={`w-28 text-right font-semibold tabular-nums ${voided ? 'text-neutral-400 line-through' : t.type === 'EXPENSE' ? 'text-danger-700' : ''}`}>
                        {t.type === 'EXPENSE' ? '− ' : ''}
                        {formatCurrency(t.amount)}
                      </span>
                      <Button variant="ghost" aria-label={`Comprobante ${t.operationNumber}`} icon={<Receipt aria-hidden className="size-4" />}
                        onClick={() => navigate(`/comprobantes-movimiento/${t.id}`)} />
                    </li>
                  )
                })}
              </ul>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
