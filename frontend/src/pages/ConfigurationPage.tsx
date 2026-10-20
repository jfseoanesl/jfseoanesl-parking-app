import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Plus, Save } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { ParkingConfiguration, PaymentMethod, PaymentMethodCategory, ReceiptFormat, VehicleType } from '../api/types'
import { DEFAULT_RECEIPT_FORMAT, parseReceiptFormat } from '../lib/receiptFormat'
import { Button } from '../components/Button'
import { Alert, LoadingState } from '../components/Feedback'
import { SelectField, TextField } from '../components/Form'
import { Card, PageHeader } from '../components/Layout'
import { VehicleIcon } from '../components/Badges'
import { useToast } from '../components/Toast'

/** UC-013 (RF-010 a RF-015). Wireframe: 09-ux-specification.md §5.11. */
export default function ConfigurationPage() {
  const toast = useToast()
  const [config, setConfig] = useState<ParkingConfiguration | null>(null)
  const [vehicleTypes, setVehicleTypes] = useState<VehicleType[]>([])
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const [parkingName, setParkingName] = useState('')
  const [nit, setNit] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [maxCapacity, setMaxCapacity] = useState('')
  const [timezone, setTimezone] = useState('America/Bogota')
  const [currency, setCurrency] = useState('COP')
  const [graceDays, setGraceDays] = useState('3')
  const [receiptFormat, setReceiptFormat] = useState<ReceiptFormat>(DEFAULT_RECEIPT_FORMAT)

  function load() {
    Promise.all([
      api.get<ParkingConfiguration>('/configuration'),
      api.get<VehicleType[]>('/vehicle-types'),
      api.get<PaymentMethod[]>('/payment-methods'),
    ])
      .then(([c, vt, pm]) => {
        setConfig(c)
        setParkingName(c.parkingName)
        setNit(c.nit ?? '')
        setAddress(c.address ?? '')
        setPhone(c.phone ?? '')
        setMaxCapacity(c.maxCapacity != null ? String(c.maxCapacity) : '')
        setTimezone(c.timezone)
        setCurrency(c.currency)
        setGraceDays(String(c.subscriptionGraceDays ?? 3))
        setReceiptFormat(parseReceiptFormat(c.receiptFormatJson))
        setVehicleTypes(vt)
        setPaymentMethods(pm)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar la configuración'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.put('/configuration', {
        parkingName,
        nit: nit || null,
        address: address || null,
        phone: phone || null,
        maxCapacity: maxCapacity ? Number(maxCapacity) : null,
        timezone,
        currency,
        // Se envía siempre: omitirlo borraba el formato del comprobante en cada guardado (RF-015).
        receiptFormatJson: JSON.stringify(receiptFormat),
        subscriptionGraceDays: graceDays === '' ? null : Number(graceDays),
      })
      toast('Configuración guardada')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la configuración')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingState />
  if (!config) return <Alert>{error}</Alert>

  return (
    <div>
      <PageHeader title="Configuración" description="Datos del parqueadero que aparecen en los comprobantes." />

      <div className="grid gap-4 lg:grid-cols-[3fr_2fr] lg:items-start">
        <Card title="Datos del parqueadero">
          <form onSubmit={handleSubmit} className="space-y-4">
            <TextField label="Nombre del parqueadero" value={parkingName} onChange={(e) => setParkingName(e.target.value)} required />
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField label="NIT" value={nit} onChange={(e) => setNit(e.target.value)} />
              <TextField label="Teléfono" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <TextField label="Dirección" value={address} onChange={(e) => setAddress(e.target.value)} />
            <div className="grid gap-4 sm:grid-cols-3">
              <TextField
                label="Capacidad máxima"
                type="number"
                min="0"
                value={maxCapacity}
                onChange={(e) => setMaxCapacity(e.target.value)}
                hint="Cupos totales."
              />
              <TextField label="Zona horaria" value={timezone} onChange={(e) => setTimezone(e.target.value)} />
              <TextField label="Moneda" value={currency} onChange={(e) => setCurrency(e.target.value)} />
            </div>
            <TextField
              label="Días de gracia de mensualidades"
              type="number"
              min="0"
              max="31"
              className="sm:w-72"
              value={graceDays}
              onChange={(e) => setGraceDays(e.target.value)}
              hint="Días después del corte en que la salida sigue sin cobro."
            />

            <fieldset className="border-t border-neutral-100 pt-4">
              <legend className="section-title">Formato del comprobante</legend>
              <p className="mt-1 text-sm text-neutral-500">Placa, fechas, operación, medio de pago y total siempre se imprimen.</p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {RECEIPT_TOGGLES.map(([key, label]) => (
                  <label key={key} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg border border-neutral-200 px-3 text-sm text-neutral-700 hover:bg-neutral-50">
                    <input
                      type="checkbox"
                      className="size-4 accent-primary-600"
                      checked={receiptFormat[key]}
                      onChange={(e) => setReceiptFormat((f) => ({ ...f, [key]: e.target.checked }))}
                    />
                    {label}
                  </label>
                ))}
              </div>
              <p className="mt-4 text-sm font-medium text-neutral-800">Imprimir comprobante</p>
              <p className="text-sm text-neutral-500">Al activarlo, tras el registro aparece el botón para imprimir el comprobante.</p>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                {PRINT_TOGGLES.map(([key, label]) => (
                  <label key={key} className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg border border-neutral-200 px-3 text-sm text-neutral-700 hover:bg-neutral-50">
                    <input
                      type="checkbox"
                      className="size-4 accent-primary-600"
                      checked={receiptFormat[key]}
                      onChange={(e) => setReceiptFormat((f) => ({ ...f, [key]: e.target.checked }))}
                    />
                    {label}
                  </label>
                ))}
              </div>
              <TextField
                label="Pie de página"
                className="mt-4"
                value={receiptFormat.footer}
                maxLength={200}
                onChange={(e) => setReceiptFormat((f) => ({ ...f, footer: e.target.value }))}
                placeholder="Ej. Gracias por su visita"
              />
            </fieldset>

            {error && <Alert>{error}</Alert>}

            <div className="flex justify-end">
              <Button type="submit" loading={submitting} icon={<Save aria-hidden className="size-5" />}>
                Guardar cambios
              </Button>
            </div>
          </form>
        </Card>

        <div className="space-y-4">
          <VehicleTypeCatalog vehicleTypes={vehicleTypes} onChanged={load} />
          <PaymentMethodCatalog paymentMethods={paymentMethods} onChanged={load} />
        </div>
      </div>
    </div>
  )
}

type ReceiptToggle = Exclude<keyof ReceiptFormat, 'footer'>

const PRINT_TOGGLES: [ReceiptToggle, string][] = [
  ['printOnEntry', 'Al registrar el ingreso'],
  ['printOnExit', 'Al registrar la salida (cobro)'],
]

const RECEIPT_TOGGLES: [ReceiptToggle, string][] = [
  ['showNit', 'Mostrar NIT'],
  ['showAddress', 'Mostrar dirección'],
  ['showPhone', 'Mostrar teléfono'],
  ['showTariff', 'Mostrar tarifa aplicada'],
  ['showDuration', 'Mostrar permanencia'],
  ['showEmployee', 'Mostrar empleado que atendió'],
]

function Chip({ children }: { children: ReactNode }) {
  return (
    <li className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-1.5 text-sm text-neutral-700">
      {children}
    </li>
  )
}

function VehicleTypeCatalog({ vehicleTypes, onChanged }: { vehicleTypes: VehicleType[]; onChanged: () => void }) {
  const toast = useToast()
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleAdd(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.post('/vehicle-types', { code, name })
      toast(`Tipo "${name}" agregado`)
      setCode('')
      setName('')
      onChanged()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear el tipo de vehículo')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card title="Tipos de vehículo">
      <ul className="mb-4 flex flex-wrap gap-2">
        {vehicleTypes.map((vt) => (
          <Chip key={vt.id}>
            <VehicleIcon code={vt.code} className="size-4 text-neutral-500" />
            {vt.name}
            <span className="font-mono text-xs text-neutral-500">{vt.code}</span>
          </Chip>
        ))}
      </ul>
      <form onSubmit={handleAdd} className="space-y-3">
        <div className="grid grid-cols-[7rem_1fr] gap-3">
          <TextField label="Código" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="VAN" required />
          <TextField label="Nombre" value={name} onChange={(e) => setName(e.target.value)} placeholder="Camioneta" required />
        </div>
        {error && <Alert>{error}</Alert>}
        <Button type="submit" variant="secondary" fullWidth loading={submitting} icon={<Plus aria-hidden className="size-4" />}>
          Agregar tipo
        </Button>
      </form>
    </Card>
  )
}

const CATEGORY_LABELS: Record<PaymentMethodCategory, string> = {
  CASH: 'Efectivo',
  ELECTRONIC: 'Electrónico',
  OTHER: 'Otro',
}

function PaymentMethodCatalog({ paymentMethods, onChanged }: { paymentMethods: PaymentMethod[]; onChanged: () => void }) {
  const toast = useToast()
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [category, setCategory] = useState<PaymentMethodCategory>('OTHER')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleAdd(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.post('/payment-methods', { code, name, category })
      toast(`Medio de pago "${name}" agregado`)
      setCode('')
      setName('')
      setCategory('OTHER')
      onChanged()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear el medio de pago')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card title="Medios de pago" description="Solo la categoría Efectivo cuenta en el arqueo de caja.">
      <ul className="mb-4 flex flex-wrap gap-2">
        {paymentMethods.map((pm) => (
          <Chip key={pm.id}>{pm.name}</Chip>
        ))}
      </ul>
      <form onSubmit={handleAdd} className="space-y-3">
        <div className="grid grid-cols-[7rem_1fr] gap-3">
          <TextField label="Código" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="NEQUI" required />
          <TextField label="Nombre" value={name} onChange={(e) => setName(e.target.value)} placeholder="Nequi" required />
        </div>
        <SelectField label="Categoría" value={category} onChange={(e) => setCategory(e.target.value as PaymentMethodCategory)}>
          {(Object.keys(CATEGORY_LABELS) as PaymentMethodCategory[]).map((c) => (
            <option key={c} value={c}>
              {CATEGORY_LABELS[c]}
            </option>
          ))}
        </SelectField>
        {error && <Alert>{error}</Alert>}
        <Button type="submit" variant="secondary" fullWidth loading={submitting} icon={<Plus aria-hidden className="size-4" />}>
          Agregar medio de pago
        </Button>
      </form>
    </Card>
  )
}
