import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Clock, Pencil, Plus, Power, Sun, SunMoon, Moon, Tags, Timer, X } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { AdminTariff, TariffModality, TariffRounding, VehicleType } from '../api/types'
import { formatCurrency, formatDate, localIsoDate } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { ChoiceGroup, SelectField, TextField } from '../components/Form'
import { Card, PageHeader } from '../components/Layout'
import { Badge, VehicleIcon } from '../components/Badges'
import { useToast } from '../components/Toast'

const MODALITY_LABELS: Record<TariffModality, string> = {
  FULL_AM: 'Plena AM',
  FULL_PM: 'Plena PM',
  FULL_DAY: 'Todo el día',
  BY_TIME: 'Por tiempo',
}

const MODALITY_ICONS: Record<TariffModality, ReactNode> = {
  FULL_AM: <Sun aria-hidden className="size-5" />,
  FULL_PM: <Moon aria-hidden className="size-5" />,
  FULL_DAY: <SunMoon aria-hidden className="size-5" />,
  BY_TIME: <Timer aria-hidden className="size-5" />,
}

const NEEDS_WINDOW: TariffModality[] = ['FULL_AM', 'FULL_PM', 'FULL_DAY']

/** UC-008 (RF-018 a RF-022). Wireframe: 09-ux-specification.md §5.9. */
export default function TariffsPage() {
  const toast = useToast()
  const [tariffs, setTariffs] = useState<AdminTariff[]>([])
  const [vehicleTypes, setVehicleTypes] = useState<VehicleType[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [minTimeMinutes, setMinTimeMinutes] = useState('')
  const [validTo, setValidTo] = useState('')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [modality, setModality] = useState<TariffModality>('BY_TIME')
  const [value, setValue] = useState('')
  const [firstFractionValue, setFirstFractionValue] = useState('')
  const [startTime, setStartTime] = useState('06:00')
  const [endTime, setEndTime] = useState('18:00')
  const [toleranceMinutes, setToleranceMinutes] = useState('10')
  const [fractionMinutes, setFractionMinutes] = useState('60')
  const [rounding, setRounding] = useState<TariffRounding>('UP')
  const [priority, setPriority] = useState('1')
  const [validFrom, setValidFrom] = useState(localIsoDate())

  function load() {
    Promise.all([api.get<AdminTariff[]>('/tariffs'), api.get<VehicleType[]>('/vehicle-types')])
      .then(([t, vt]) => {
        setTariffs(t)
        setVehicleTypes(vt)
        if (vt.length > 0 && !vehicleTypeId) setVehicleTypeId(vt[0].id)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las tarifas'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  function resetForm() {
    setEditingId(null)
    setName('')
    setDescription('')
    setMinTimeMinutes('')
    setValidTo('')
    setModality('BY_TIME')
    setValue('')
    setFirstFractionValue('')
    setToleranceMinutes('10')
    setFractionMinutes('60')
    setRounding('UP')
    setPriority('1')
    setValidFrom(localIsoDate())
    setError(null)
  }

  function startEdit(tariff: AdminTariff) {
    setEditingId(tariff.id)
    setName(tariff.name)
    setDescription(tariff.description ?? '')
    setMinTimeMinutes(tariff.minTimeMinutes != null ? String(tariff.minTimeMinutes) : '')
    setValidTo(tariff.validTo ?? '')
    setVehicleTypeId(tariff.vehicleTypeId)
    setModality(tariff.modality)
    setValue(String(tariff.value))
    setFirstFractionValue(tariff.firstFractionValue != null ? String(tariff.firstFractionValue) : '')
    setStartTime(tariff.startTime?.slice(0, 5) ?? '06:00')
    setEndTime(tariff.endTime?.slice(0, 5) ?? '18:00')
    setToleranceMinutes(tariff.toleranceMinutes != null ? String(tariff.toleranceMinutes) : '')
    setFractionMinutes(tariff.fractionMinutes != null ? String(tariff.fractionMinutes) : '')
    setRounding(tariff.rounding)
    setPriority(String(tariff.priority))
    setValidFrom(tariff.validFrom)
    setError(null)
    setShowForm(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const body = {
        name,
        description: description.trim() || null,
        vehicleTypeId,
        modality,
        value: Number(value),
        firstFractionValue: modality === 'BY_TIME' && firstFractionValue ? Number(firstFractionValue) : null,
        startTime: NEEDS_WINDOW.includes(modality) ? `${startTime}:00` : null,
        endTime: NEEDS_WINDOW.includes(modality) ? `${endTime}:00` : null,
        toleranceMinutes: toleranceMinutes ? Number(toleranceMinutes) : null,
        minTimeMinutes: modality === 'BY_TIME' && minTimeMinutes ? Number(minTimeMinutes) : null,
        fractionMinutes: modality === 'BY_TIME' ? Number(fractionMinutes) : null,
        timeUnit: modality === 'BY_TIME' ? 'MINUTES' : null,
        rounding,
        priority: Number(priority),
        validFrom,
        validTo: validTo || null,
      }
      if (editingId) {
        await api.put(`/tariffs/${editingId}`, body)
      } else {
        await api.post('/tariffs', body)
      }
      toast(editingId ? `Tarifa "${name}" actualizada` : `Tarifa "${name}" creada`)
      resetForm()
      setShowForm(false)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar la tarifa')
    } finally {
      setSubmitting(false)
    }
  }

  async function toggleActive(tariff: AdminTariff) {
    try {
      await api.patch(`/tariffs/${tariff.id}/status`, { active: !tariff.active })
      toast(`Tarifa "${tariff.name}" ${tariff.active ? 'desactivada' : 'activada'}`)
      load()
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado', 'danger')
    }
  }

  const vehicleTypeById = new Map(vehicleTypes.map((vt) => [vt.id, vt]))

  return (
    <div>
      <PageHeader
        title="Tarifas"
        description="Si varias tarifas activas aplican, gana la de número de prioridad más bajo (1 = máxima)."
        actions={
          !showForm && (
            <Button
              icon={<Plus aria-hidden className="size-5" />}
              onClick={() => {
                resetForm()
                setShowForm(true)
              }}
            >
              Nueva tarifa
            </Button>
          )
        }
      />

      {showForm && (
        <Card
          className="mb-6"
          title={editingId ? 'Editar tarifa' : 'Nueva tarifa'}
          actions={
            <button
              type="button"
              aria-label="Cerrar formulario"
              onClick={() => {
                resetForm()
                setShowForm(false)
              }}
              className="flex size-11 items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <X aria-hidden className="size-5" />
            </button>
          }
        >
          <form onSubmit={handleSubmit} className="space-y-6">
            <FormSection title="Identificación">
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Nombre" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ej. Por tiempo - Auto" required />
                <SelectField label="Tipo de vehículo" value={vehicleTypeId} onChange={(e) => setVehicleTypeId(e.target.value)}>
                  {vehicleTypes.map((vt) => (
                    <option key={vt.id} value={vt.id}>
                      {vt.name}
                    </option>
                  ))}
                </SelectField>
                <TextField
                  label="Descripción (opcional)"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Ej. Aplica de lunes a domingo"
                  maxLength={255}
                  className="sm:col-span-2"
                />
              </div>
            </FormSection>

            <FormSection title="Condición horaria">
              <ChoiceGroup
                label="Modalidad"
                columns={4}
                value={modality}
                onChange={setModality}
                options={(Object.keys(MODALITY_LABELS) as TariffModality[]).map((m) => ({
                  value: m,
                  label: MODALITY_LABELS[m],
                  icon: MODALITY_ICONS[m],
                }))}
              />
              {NEEDS_WINDOW.includes(modality) && (
                <div className="mt-4 grid grid-cols-2 gap-4">
                  <TextField label="Hora inicial" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
                  <TextField label="Hora final" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} required />
                </div>
              )}
            </FormSection>

            <FormSection title="Cálculo">
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField
                  label={modality === 'BY_TIME' && firstFractionValue ? 'Valor por fracción adicional' : 'Valor'}
                  type="number"
                  inputMode="numeric"
                  min="0"
                  adornment="$"
                  value={value}
                  onChange={(e) => setValue(e.target.value)}
                  required
                />
                <TextField
                  label="Tolerancia (min)"
                  type="number"
                  min="0"
                  value={toleranceMinutes}
                  onChange={(e) => setToleranceMinutes(e.target.value)}
                  hint="Minutos de gracia sin cobro."
                />
                {modality === 'BY_TIME' && (
                  <>
                    <TextField label="Fracción (min)" type="number" min="1" value={fractionMinutes} onChange={(e) => setFractionMinutes(e.target.value)} required />
                    <TextField
                      label="Valor de la primera fracción"
                      type="number"
                      inputMode="numeric"
                      min="0"
                      adornment="$"
                      value={firstFractionValue}
                      onChange={(e) => setFirstFractionValue(e.target.value)}
                      hint="Opcional. Ej.: primera hora $4.000 y cada hora adicional o fracción $1.000. Vacío: todas valen lo mismo."
                    />
                    <TextField
                      label="Tiempo mínimo a cobrar (min)"
                      type="number"
                      min="0"
                      value={minTimeMinutes}
                      onChange={(e) => setMinTimeMinutes(e.target.value)}
                      hint="Opcional. Pasada la tolerancia, se cobra al menos este tiempo."
                    />
                    <SelectField label="Redondeo" value={rounding} onChange={(e) => setRounding(e.target.value as TariffRounding)}>
                      <option value="UP">Hacia arriba</option>
                      <option value="DOWN">Hacia abajo</option>
                      <option value="NEAREST">Al más cercano</option>
                    </SelectField>
                  </>
                )}
              </div>
            </FormSection>

            <FormSection title="Vigencia y prioridad">
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Vigente desde" type="date" value={validFrom} onChange={(e) => setValidFrom(e.target.value)} required />
                <TextField
                  label="Vigente hasta (opcional)"
                  type="date"
                  value={validTo}
                  min={validFrom}
                  onChange={(e) => setValidTo(e.target.value)}
                  hint="Vacío = sin fecha de fin."
                />
                <TextField
                  label="Prioridad"
                  type="number"
                  min="1"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  hint="1 = máxima prioridad. Si dos coinciden con igual prioridad, se cobra la de menor valor (BR-008)."
                  required
                />
              </div>
            </FormSection>

            {error && <Alert>{error}</Alert>}

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button
                variant="secondary"
                onClick={() => {
                  resetForm()
                  setShowForm(false)
                }}
              >
                Cancelar
              </Button>
              <Button type="submit" loading={submitting}>
                {editingId ? 'Guardar cambios' : 'Crear tarifa'}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {loading ? (
        <div className="grid gap-3 md:grid-cols-2">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36" />
          ))}
        </div>
      ) : tariffs.length === 0 ? (
        <EmptyState
          icon={<Tags aria-hidden className="size-6" />}
          title="Aún no hay tarifas"
          description="Crea la primera tarifa para poder cobrar las salidas."
        />
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {tariffs.map((tariff) => {
            const vt = vehicleTypeById.get(tariff.vehicleTypeId)
            return (
              <li key={tariff.id} className={`card flex flex-col p-5 ${tariff.active ? '' : 'bg-neutral-50'}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className={`truncate font-semibold ${tariff.active ? 'text-neutral-900' : 'text-neutral-500'}`}>{tariff.name}</p>
                    <p className="mt-1 flex items-center gap-1.5 text-sm text-neutral-600">
                      <VehicleIcon code={vt?.code} className="size-4" />
                      {vt?.name ?? 'Vehículo'}
                    </p>
                    {tariff.description && <p className="mt-1 text-sm text-neutral-500">{tariff.description}</p>}
                  </div>
                  <Badge tone={tariff.active ? 'success' : 'neutral'}>{tariff.active ? 'Activa' : 'Inactiva'}</Badge>
                </div>

                <p className="mt-4 text-2xl font-semibold text-neutral-900">
                  {tariff.modality === 'BY_TIME' && tariff.firstFractionValue != null ? (
                    <>
                      {formatCurrency(tariff.firstFractionValue)}
                      <span className="text-sm font-normal text-neutral-500"> primeros {tariff.fractionMinutes} min · luego </span>
                      {formatCurrency(tariff.value)}
                    </>
                  ) : (
                    formatCurrency(tariff.value)
                  )}
                  {tariff.modality === 'BY_TIME' && tariff.fractionMinutes && (
                    <span className="text-sm font-normal text-neutral-500"> / {tariff.fractionMinutes} min</span>
                  )}
                </p>

                <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-neutral-600">
                  <span className="inline-flex items-center gap-1.5">
                    <span className="text-neutral-500">{MODALITY_ICONS[tariff.modality]}</span>
                    {MODALITY_LABELS[tariff.modality]}
                  </span>
                  {tariff.startTime && (
                    <span className="inline-flex items-center gap-1.5 tabular-nums">
                      <Clock aria-hidden className="size-4 text-neutral-500" />
                      {tariff.startTime.slice(0, 5)}–{tariff.endTime?.slice(0, 5)}
                    </span>
                  )}
                  <span>Prioridad {tariff.priority}</span>
                  <span className="text-neutral-500">
                    Desde {formatDate(tariff.validFrom)}
                    {tariff.validTo && <> hasta {formatDate(tariff.validTo)}</>}
                  </span>
                  {tariff.minTimeMinutes != null && <span>Mínimo {tariff.minTimeMinutes} min</span>}
                </div>

                <div className="mt-4 flex gap-2 border-t border-neutral-100 pt-4">
                  <Button variant="secondary" icon={<Pencil aria-hidden className="size-4" />} onClick={() => startEdit(tariff)}>
                    Editar
                  </Button>
                  <Button variant="ghost" icon={<Power aria-hidden className="size-4" />} onClick={() => toggleActive(tariff)}>
                    {tariff.active ? 'Desactivar' : 'Activar'}
                  </Button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="border-t border-neutral-100 pt-5 first:border-t-0 first:pt-0">
      <legend className="section-title float-left mb-3 w-full">{title}</legend>
      <div className="clear-both">{children}</div>
    </fieldset>
  )
}
