import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronDown, CircleCheck, Printer, TriangleAlert } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { ParkingConfiguration, ParkingSession, VehicleType } from '../api/types'
import { DEFAULT_RECEIPT_FORMAT, parseReceiptFormat } from '../lib/receiptFormat'
import { detectPlateType, plateFormatIssue } from '../lib/plateFormat'
import { PlateBadge } from '../components/Badges'
import { Button } from '../components/Button'
import { Alert, Skeleton } from '../components/Feedback'
import { ChoiceGroup } from '../components/Form'
import { PageHeader } from '../components/Layout'
import { VehicleIcon } from '../components/Badges'
import { useToast } from '../components/Toast'

/** BR-002: la placa se normaliza al escribir (mayusculas, sin espacios). */
function normalizePlate(value: string): string {
  return value.toUpperCase().replace(/\s+/g, '')
}

/** UC-002 (RF-023 a RF-029). Wireframe: 09-ux-specification.md §5.3. */
export default function NewEntryPage() {
  const toast = useToast()
  const plateRef = useRef<HTMLInputElement>(null)
  const [vehicleTypes, setVehicleTypes] = useState<VehicleType[] | null>(null)
  const [plate, setPlate] = useState('')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  /** El usuario eligió el tipo a mano: ya no se ajusta solo según la placa (BR-036). */
  const [typeChosen, setTypeChosen] = useState(false)
  const [showNotes, setShowNotes] = useState(false)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()
  const [printOnEntry, setPrintOnEntry] = useState(DEFAULT_RECEIPT_FORMAT.printOnEntry)
  /** Último ingreso registrado, para ofrecer su comprobante (RF-072) sin bloquear el siguiente. */
  const [lastEntry, setLastEntry] = useState<ParkingSession | null>(null)

  useEffect(() => {
    api
      .get<ParkingConfiguration>('/configuration')
      .then((c) => setPrintOnEntry(parseReceiptFormat(c.receiptFormatJson).printOnEntry))
      .catch(() => undefined)
    api
      .get<VehicleType[]>('/vehicle-types')
      .then((types) => {
        setVehicleTypes(types)
        if (types.length > 0) setVehicleTypeId(types[0].id)
      })
      .catch(() => {
        setVehicleTypes([])
        setError('No se pudieron cargar los tipos de vehículo')
      })
  }, [])

  const selectedType = vehicleTypes?.find((t) => t.id === vehicleTypeId)
  // BR-036: el aviso aparece desde 5 caracteres (largo mínimo de una placa de moto antigua).
  const formatIssue = plate.length >= 5 ? plateFormatIssue(plate, selectedType?.code) : null
  const suggestedType = formatIssue?.suggested ? vehicleTypes?.find((t) => t.code === formatIssue.suggested) : undefined

  function handlePlateChange(value: string) {
    const normalized = normalizePlate(value)
    setPlate(normalized)
    // Si el usuario no eligió el tipo, se ajusta al que corresponde a la placa.
    const detected = detectPlateType(normalized)
    const match = detected ? vehicleTypes?.find((t) => t.code === detected) : undefined
    if (!typeChosen && match) setVehicleTypeId(match.id)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const created = await api.post<ParkingSession>('/parking-sessions', {
        plate,
        vehicleTypeId,
        notes: notes || undefined,
      })
      toast(`Ingreso registrado: ${created.plate}`)
      // BR-029: cliente mensual reconocido, o aviso de que su mensualidad ya tiene otro vehiculo dentro.
      if (created.notice) toast(created.notice, created.subscriptionId ? 'success' : 'warning')
      setLastEntry(created)
      // Lista para el siguiente ingreso sin navegar (RNF-001): se conserva el tipo elegido.
      setPlate('')
      setTypeChosen(false)
      setNotes('')
      setShowNotes(false)
      plateRef.current?.focus()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar el ingreso')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <PageHeader title="Nuevo ingreso" description="Escribe la placa, elige el tipo y confirma." />

      <form onSubmit={handleSubmit} className="card space-y-6 p-5 sm:p-6">
        <div>
          <label className="label" htmlFor="plate">
            Placa
          </label>
          <input
            id="plate"
            ref={plateRef}
            autoFocus
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            maxLength={10}
            className="block min-h-16 w-full rounded-xl border-2 border-neutral-900 bg-plate-yellow px-3 text-center font-mono text-4xl font-bold tracking-[0.2em] text-neutral-900 uppercase transition-shadow duration-150 placeholder:text-neutral-900/25 focus:ring-4 focus:ring-primary-200 focus:outline-none"
            value={plate}
            onChange={(e) => handlePlateChange(e.target.value)}
            aria-describedby={formatIssue ? 'plate-format' : undefined}
            aria-invalid={formatIssue ? true : undefined}
            placeholder="ABC123"
            required
          />
          <p className="mt-1.5 text-xs text-neutral-500">Se convierte a mayúsculas y sin espacios automáticamente.</p>
        </div>

        {vehicleTypes === null ? (
          <div>
            <Skeleton className="mb-2 h-4 w-32" />
            <div className="grid grid-cols-2 gap-2">
              <Skeleton className="h-12" />
              <Skeleton className="h-12" />
            </div>
          </div>
        ) : (
          <ChoiceGroup
            label="Tipo de vehículo"
            value={vehicleTypeId}
            onChange={(id) => {
              setVehicleTypeId(id)
              setTypeChosen(true)
            }}
            options={vehicleTypes.map((type) => ({
              value: type.id,
              label: type.name,
              icon: <VehicleIcon code={type.code} className="size-6" />,
            }))}
          />
        )}

        <div>
          {showNotes ? (
            <>
              <label className="label" htmlFor="notes">
                Observaciones (opcional)
              </label>
              <textarea
                id="notes"
                rows={2}
                autoFocus
                className="input py-2"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ej. rayón en la puerta trasera"
              />
            </>
          ) : (
            <button
              type="button"
              onClick={() => setShowNotes(true)}
              aria-expanded={false}
              className="flex min-h-11 items-center gap-1.5 rounded-lg text-sm font-medium text-primary-700 hover:text-primary-800"
            >
              <ChevronDown aria-hidden className="size-4" />
              Agregar observaciones
            </button>
          )}
        </div>

        {formatIssue && (
          <div id="plate-format" role="alert" className="space-y-3 rounded-xl border border-warning-200 bg-warning-50 p-4 text-sm text-warning-800">
            <p className="flex items-start gap-2">
              <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0" />
              {formatIssue.message}.
              {!formatIssue.suggested && ' Formato esperado: ABC123 (automóvil) o ABC12D / ABC12 (motocicleta).'}
            </p>
            {suggestedType && (
              <Button
                variant="secondary"
                onClick={() => {
                  setVehicleTypeId(suggestedType.id)
                  setTypeChosen(true)
                }}
              >
                Cambiar a {suggestedType.name}
              </Button>
            )}
          </div>
        )}

        {error && <Alert>{error}</Alert>}

        <Button type="submit" size="lg" fullWidth loading={submitting} disabled={!vehicleTypeId || !plate || formatIssue !== null}>
          {submitting ? 'Registrando…' : 'Confirmar ingreso'}
        </Button>
      </form>

      {printOnEntry && lastEntry && (
        <div aria-live="polite" className="card mt-4 flex flex-wrap items-center gap-3 p-4">
          <CircleCheck aria-hidden className="size-6 shrink-0 text-success-600" />
          <PlateBadge plate={lastEntry.plate} size="sm" />
          <span className="min-w-0 flex-1 text-sm text-neutral-600">
            Ingreso <span className="font-mono font-medium text-neutral-900">{lastEntry.entryNumber ?? ''}</span>
          </span>
          <Button
            variant="secondary"
            icon={<Printer aria-hidden className="size-5" />}
            onClick={() => navigate(`/comprobantes-ingreso/${lastEntry.id}`)}
          >
            Imprimir comprobante
          </Button>
        </div>
      )}
    </div>
  )
}
