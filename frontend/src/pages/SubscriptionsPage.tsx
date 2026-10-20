import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CalendarCheck, Car, ChevronDown, Pencil, Plus, Power, Receipt, Search, Trash2, Wallet, X } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { PaymentMethod, Subscription, SubscriptionPayment, VehicleType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { formatCurrency, formatDate, formatDateTime } from '../lib/format'
import { matchesSubscription, SUBSCRIPTION_STATUS } from '../lib/subscription'
import { Button } from '../components/Button'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { SelectField, TextField } from '../components/Form'
import { Card, PageHeader } from '../components/Layout'
import { Badge, PlateBadge } from '../components/Badges'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'

interface VehicleRow {
  plate: string
  vehicleTypeId: string
}

interface FormState {
  customerName: string
  documentId: string
  phone: string
  monthlyFee: string
  cutOffDay: string
  notes: string
  vehicles: VehicleRow[]
}

const EMPTY_FORM: FormState = { customerName: '', documentId: '', phone: '', monthlyFee: '', cutOffDay: '5', notes: '', vehicles: [] }

/**
 * RF-074 a RF-077: mensualidades. Todos consultan y cobran (con caja abierta); solo el
 * administrador crea, edita y activa/desactiva.
 */
export default function SubscriptionsPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [subscriptions, setSubscriptions] = useState<Subscription[] | null>(null)
  const [vehicleTypes, setVehicleTypes] = useState<VehicleType[]>([])
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [paying, setPaying] = useState<Subscription | null>(null)
  const [paymentMethodId, setPaymentMethodId] = useState('')
  const [payError, setPayError] = useState<string | null>(null)
  const [needsCash, setNeedsCash] = useState(false)
  const [paySubmitting, setPaySubmitting] = useState(false)

  function load() {
    api
      .get<Subscription[]>('/subscriptions')
      .then(setSubscriptions)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las mensualidades'))
  }

  useEffect(() => {
    load()
    api.get<VehicleType[]>('/vehicle-types').then(setVehicleTypes).catch(() => undefined)
    api
      .get<PaymentMethod[]>('/payment-methods')
      .then(setPaymentMethods)
      .catch(() => undefined)
  }, [])

  const visible = useMemo(() => (subscriptions ?? []).filter((s) => matchesSubscription(s, query)), [subscriptions, query])
  const vehicleTypeName = (id: string) => vehicleTypes.find((t) => t.id === id)?.name ?? 'Vehículo'

  function openCreate() {
    setEditingId(null)
    setForm({ ...EMPTY_FORM, vehicles: [{ plate: '', vehicleTypeId: vehicleTypes[0]?.id ?? '' }] })
    setFormError(null)
    setShowForm(true)
  }

  function openEdit(s: Subscription) {
    setEditingId(s.id)
    setForm({
      customerName: s.customerName,
      documentId: s.documentId ?? '',
      phone: s.phone ?? '',
      monthlyFee: String(s.monthlyFee),
      cutOffDay: String(s.cutOffDay),
      notes: s.notes ?? '',
      vehicles: s.vehicles.map((v) => ({ plate: v.plate, vehicleTypeId: v.vehicleTypeId })),
    })
    setFormError(null)
    setShowForm(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function setVehicle(index: number, patch: Partial<VehicleRow>) {
    setForm((f) => ({ ...f, vehicles: f.vehicles.map((v, i) => (i === index ? { ...v, ...patch } : v)) }))
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setSaving(true)
    const body = {
      customerName: form.customerName,
      documentId: form.documentId || null,
      phone: form.phone || null,
      monthlyFee: Number(form.monthlyFee),
      cutOffDay: Number(form.cutOffDay),
      notes: form.notes || null,
      vehicles: form.vehicles.filter((v) => v.plate.trim() !== ''),
    }
    try {
      if (editingId) await api.put(`/subscriptions/${editingId}`, body)
      else await api.post('/subscriptions', body)
      toast(editingId ? `Mensualidad de ${form.customerName} actualizada` : `Mensualidad de ${form.customerName} creada`)
      setShowForm(false)
      load()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar la mensualidad')
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(s: Subscription) {
    try {
      await api.patch(`/subscriptions/${s.id}/status`, { active: !s.active })
      toast(`Mensualidad de ${s.customerName} ${s.active ? 'desactivada' : 'activada'}`)
      load()
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado', 'danger')
    }
  }

  function openPayment(s: Subscription) {
    setPaying(s)
    setPaymentMethodId(paymentMethods[0]?.id ?? '')
    setPayError(null)
    setNeedsCash(false)
  }

  async function handlePay() {
    if (!paying) return
    setPaySubmitting(true)
    setPayError(null)
    try {
      const payment = await api.post<SubscriptionPayment>(`/subscriptions/${paying.id}/payments`, {
        paymentMethodId,
        amount: paying.monthlyFee,
      })
      toast(`Pago registrado: ${payment.operationNumber}`)
      setPaying(null)
      navigate(`/comprobantes-mensualidad/${payment.id}`)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'NO_OPEN_CASH_SESSION') setNeedsCash(true)
      setPayError(err instanceof ApiError ? err.message : 'No se pudo registrar el pago')
    } finally {
      setPaySubmitting(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Mensualidades"
        description="Clientes con pago mensual: sus placas autorizadas salen sin cobro mientras estén al día."
        actions={
          isAdmin &&
          !showForm && (
            <Button icon={<Plus aria-hidden className="size-5" />} onClick={openCreate}>
              Nueva mensualidad
            </Button>
          )
        }
      />

      {showForm && (
        <Card
          className="mb-6"
          title={editingId ? 'Editar mensualidad' : 'Nueva mensualidad'}
          actions={
            <button
              type="button"
              aria-label="Cerrar formulario"
              onClick={() => setShowForm(false)}
              className="flex size-11 items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <X aria-hidden className="size-5" />
            </button>
          }
        >
          <form onSubmit={handleSave} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-3">
              <TextField
                label="Cliente"
                className="sm:col-span-3"
                value={form.customerName}
                onChange={(e) => setForm({ ...form, customerName: e.target.value })}
                required
              />
              <TextField label="Documento" value={form.documentId} onChange={(e) => setForm({ ...form, documentId: e.target.value })} />
              <TextField label="Teléfono" type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              <TextField
                label="Valor mensual"
                type="number"
                inputMode="numeric"
                min="1"
                adornment="$"
                value={form.monthlyFee}
                onChange={(e) => setForm({ ...form, monthlyFee: e.target.value })}
                required
              />
              <TextField
                label="Día de corte"
                type="number"
                min="1"
                max="28"
                value={form.cutOffDay}
                onChange={(e) => setForm({ ...form, cutOffDay: e.target.value })}
                hint="Del 1 al 28. Cada pago cubre hasta el siguiente corte."
                required
              />
              <TextField
                label="Notas (opcional)"
                className="sm:col-span-2"
                value={form.notes}
                maxLength={255}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
              />
            </div>

            <fieldset>
              <legend className="label">Placas autorizadas</legend>
              <p className="mb-2 text-sm text-neutral-500">Puede entrar cualquiera de ellas, pero solo una a la vez.</p>
              <ul className="space-y-2">
                {form.vehicles.map((v, i) => (
                  <li key={i} className="flex flex-wrap items-end gap-2">
                    <TextField
                      label={`Placa ${i + 1}`}
                      className="min-w-32 flex-1"
                      value={v.plate}
                      onChange={(e) => setVehicle(i, { plate: e.target.value.toUpperCase().replace(/\s+/g, '') })}
                      autoComplete="off"
                    />
                    <SelectField
                      label="Tipo"
                      className="min-w-36 flex-1"
                      value={v.vehicleTypeId}
                      onChange={(e) => setVehicle(i, { vehicleTypeId: e.target.value })}
                    >
                      {vehicleTypes.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.name}
                        </option>
                      ))}
                    </SelectField>
                    <Button
                      variant="ghost"
                      aria-label={`Quitar placa ${i + 1}`}
                      icon={<Trash2 aria-hidden className="size-4" />}
                      onClick={() => setForm((f) => ({ ...f, vehicles: f.vehicles.filter((_, j) => j !== i) }))}
                    />
                  </li>
                ))}
              </ul>
              <Button
                variant="secondary"
                className="mt-2"
                icon={<Plus aria-hidden className="size-4" />}
                onClick={() => setForm((f) => ({ ...f, vehicles: [...f.vehicles, { plate: '', vehicleTypeId: vehicleTypes[0]?.id ?? '' }] }))}
              >
                Agregar placa
              </Button>
            </fieldset>

            {formError && <Alert>{formError}</Alert>}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button variant="secondary" onClick={() => setShowForm(false)}>
                Cancelar
              </Button>
              <Button type="submit" loading={saving}>
                {editingId ? 'Guardar cambios' : 'Crear mensualidad'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      <div className="relative mb-4">
        <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-neutral-400" />
        <input
          type="search"
          aria-label="Buscar mensualidad"
          className="input min-h-12 pl-11"
          placeholder="Buscar por cliente, documento o placa…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoComplete="off"
        />
      </div>

      {error && <Alert className="mb-4">{error}</Alert>}

      {subscriptions === null ? (
        <div className="flex flex-col gap-2">
          {[0, 1].map((i) => (
            <Skeleton key={i} className="h-36" />
          ))}
        </div>
      ) : visible.length === 0 ? (
        <EmptyState
          icon={<CalendarCheck aria-hidden className="size-6" />}
          title={query ? 'Ninguna mensualidad coincide con la búsqueda' : 'Aún no hay mensualidades'}
          description={isAdmin && !query ? 'Crea la primera con el botón "Nueva mensualidad".' : undefined}
        />
      ) : (
        <ul className="grid gap-3 lg:grid-cols-2">
          {visible.map((s) => (
            <SubscriptionCard
              key={s.id}
              subscription={s}
              isAdmin={isAdmin}
              vehicleTypeName={vehicleTypeName}
              onPay={() => openPayment(s)}
              onEdit={() => openEdit(s)}
              onToggle={() => toggleActive(s)}
            />
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={paying !== null}
        title={`Cobrar mensualidad de ${paying?.customerName ?? ''}`}
        confirmLabel={`Cobrar ${paying ? formatCurrency(paying.monthlyFee) : ''}`}
        tone="success"
        loading={paySubmitting}
        onConfirm={handlePay}
        onCancel={() => setPaying(null)}
      >
        <p>
          Valor pactado: <strong className="text-neutral-900">{paying && formatCurrency(paying.monthlyFee)}</strong> · corte el día{' '}
          {paying?.cutOffDay}. Se registra en tu caja abierta.
        </p>
        {paying?.paidUntil && <p className="mt-1">Pagada hasta {formatDate(paying.paidUntil)}; el pago extiende un periodo más.</p>}
        <SelectField label="Medio de pago" className="mt-4" value={paymentMethodId} onChange={(e) => setPaymentMethodId(e.target.value)}>
          {paymentMethods.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </SelectField>
        {payError && (
          <p className="mt-2 text-danger-700">
            {payError}{' '}
            {needsCash && (
              <Link to="/caja" className="font-medium underline">
                Ir a Caja
              </Link>
            )}
          </p>
        )}
      </ConfirmDialog>
    </div>
  )
}

function SubscriptionCard({
  subscription: s,
  isAdmin,
  vehicleTypeName,
  onPay,
  onEdit,
  onToggle,
}: {
  subscription: Subscription
  isAdmin: boolean
  vehicleTypeName: (id: string) => string
  onPay: () => void
  onEdit: () => void
  onToggle: () => void
}) {
  const navigate = useNavigate()
  const status = SUBSCRIPTION_STATUS[s.status]
  const [payments, setPayments] = useState<SubscriptionPayment[] | null>(null)
  const [showPayments, setShowPayments] = useState(false)

  function togglePayments() {
    if (!showPayments && payments === null) {
      api.get<SubscriptionPayment[]>(`/subscriptions/${s.id}/payments`).then(setPayments).catch(() => setPayments([]))
    }
    setShowPayments((v) => !v)
  }

  return (
    <li className={`card flex flex-col p-5 ${s.active ? '' : 'bg-neutral-50'}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-semibold text-neutral-900">{s.customerName}</p>
          <p className="text-sm text-neutral-500">
            {[s.documentId && `Doc. ${s.documentId}`, s.phone && `Tel. ${s.phone}`].filter(Boolean).join(' · ') || 'Sin datos de contacto'}
          </p>
        </div>
        <Badge tone={status.tone}>{status.label}</Badge>
      </div>

      <p className="mt-3 text-2xl font-semibold text-neutral-900">
        {formatCurrency(s.monthlyFee)}
        <span className="text-sm font-normal text-neutral-500"> / mes · corte día {s.cutOffDay}</span>
      </p>
      <p className="text-sm text-neutral-600">
        {s.paidUntil ? `Pagada hasta ${formatDate(s.paidUntil)}` : 'Sin pagos registrados'}
      </p>

      <ul className="mt-3 flex flex-wrap gap-2">
        {s.vehicles.map((v) => (
          <li key={v.plate} className="flex items-center gap-1.5" title={vehicleTypeName(v.vehicleTypeId)}>
            <PlateBadge plate={v.plate} size="sm" />
          </li>
        ))}
      </ul>
      {s.vehicleInsidePlate && (
        <p className="mt-2 flex items-center gap-1.5 text-sm text-primary-700">
          <Car aria-hidden className="size-4" />
          Dentro ahora: <span className="font-mono font-medium">{s.vehicleInsidePlate}</span>
        </p>
      )}
      {s.notes && <p className="mt-2 text-sm text-neutral-500">{s.notes}</p>}

      <div className="mt-4 flex flex-wrap gap-2 border-t border-neutral-100 pt-4">
        {s.active && (
          <Button icon={<Wallet aria-hidden className="size-4" />} onClick={onPay}>
            Registrar pago
          </Button>
        )}
        <Button
          variant="ghost"
          aria-expanded={showPayments}
          icon={<ChevronDown aria-hidden className={`size-4 transition-transform ${showPayments ? 'rotate-180' : ''}`} />}
          onClick={togglePayments}
        >
          Pagos
        </Button>
        {isAdmin && (
          <>
            <Button variant="secondary" icon={<Pencil aria-hidden className="size-4" />} onClick={onEdit}>
              Editar
            </Button>
            <Button variant="ghost" icon={<Power aria-hidden className="size-4" />} onClick={onToggle}>
              {s.active ? 'Desactivar' : 'Activar'}
            </Button>
          </>
        )}
      </div>

      {showPayments && (
        <div className="mt-3 rounded-lg bg-neutral-50 p-3 text-sm">
          {payments === null ? (
            <Skeleton className="h-10" />
          ) : payments.length === 0 ? (
            <p className="text-neutral-500">Sin pagos registrados.</p>
          ) : (
            <ul className="divide-y divide-neutral-200">
              {payments.map((p) => (
                <li key={p.id} className="flex flex-wrap items-center gap-2 py-2">
                  <span className="font-mono text-neutral-700">{p.operationNumber}</span>
                  <span className="flex-1 text-neutral-600">
                    {formatDate(p.periodFrom)} – {formatDate(p.periodTo)} · {formatDateTime(p.paidAt)}
                  </span>
                  <span className="font-medium tabular-nums">{formatCurrency(p.amount)}</span>
                  <Button
                    variant="ghost"
                    aria-label={`Comprobante ${p.operationNumber}`}
                    icon={<Receipt aria-hidden className="size-4" />}
                    onClick={() => navigate(`/comprobantes-mensualidad/${p.id}`)}
                  />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </li>
  )
}
