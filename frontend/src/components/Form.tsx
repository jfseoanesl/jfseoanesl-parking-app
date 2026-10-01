import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { Check } from 'lucide-react'

interface FieldProps {
  label: string
  hint?: ReactNode
  /** Adorno a la izquierda del input (ej. "$" o un icono). */
  adornment?: ReactNode
}

/** Input con label asociado por id (form-labels): nunca el placeholder como unica etiqueta. */
export function TextField({ label, hint, adornment, id, className = '', ...rest }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hintId = hint ? `${inputId}-hint` : undefined
  return (
    <div className={className}>
      <label className="label" htmlFor={inputId}>
        {label}
      </label>
      <div className="relative">
        {adornment && (
          <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-neutral-500">
            {adornment}
          </span>
        )}
        <input id={inputId} aria-describedby={hintId} className={`input ${adornment ? 'pl-9' : ''}`} {...rest} />
      </div>
      {hint && (
        <p id={hintId} className="mt-1.5 text-xs text-neutral-500">
          {hint}
        </p>
      )}
    </div>
  )
}

export function SelectField({ label, hint, id, className = '', children, ...rest }: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  return (
    <div className={className}>
      <label className="label" htmlFor={selectId}>
        {label}
      </label>
      <select id={selectId} className="input" {...rest}>
        {children}
      </select>
      {hint && <p className="mt-1.5 text-xs text-neutral-500">{hint}</p>}
    </div>
  )
}

/**
 * Grupo de opciones grandes en lugar de un dropdown (09-ux-specification.md §5.3/§5.5): un toque
 * por seleccion. Cada opcion es un boton con aria-pressed, asi el estado no depende solo del color.
 */
export function ChoiceGroup<T extends string>({ label, options, value, onChange, columns = 2 }: {
  label: string
  options: { value: T; label: string; icon?: ReactNode; description?: string }[]
  value: T
  onChange: (value: T) => void
  columns?: 2 | 3 | 4
}) {
  const cols = { 2: 'grid-cols-2', 3: 'grid-cols-2 sm:grid-cols-3', 4: 'grid-cols-2 sm:grid-cols-4' }[columns]
  return (
    <fieldset>
      <legend className="label">{label}</legend>
      <div className={`grid gap-2 ${cols}`}>
        {options.map((option) => {
          const selected = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              aria-pressed={selected}
              onClick={() => onChange(option.value)}
              className={`relative flex min-h-12 items-center gap-2.5 rounded-lg border px-3 py-2 text-left text-sm font-medium transition-colors duration-150 ${
                selected
                  ? 'border-primary-600 bg-primary-50 text-primary-800 ring-1 ring-primary-600'
                  : 'border-neutral-300 bg-white text-neutral-700 hover:border-neutral-400 hover:bg-neutral-50'
              }`}
            >
              {option.icon && (
                <span className={selected ? 'text-primary-600' : 'text-neutral-500'}>{option.icon}</span>
              )}
              <span className="min-w-0 flex-1">
                {option.label}
                {option.description && (
                  <span className="block text-xs font-normal text-neutral-500">{option.description}</span>
                )}
              </span>
              {selected && <Check aria-hidden className="size-4 shrink-0 text-primary-600" />}
            </button>
          )
        })}
      </div>
    </fieldset>
  )
}

/** Pestañas segmentadas (ej. presets de reportes). */
export function SegmentedControl<T extends string>({ label, options, value, onChange }: {
  label: string
  options: { value: T; label: string }[]
  value: T
  onChange: (value: T) => void
}) {
  return (
    <div role="group" aria-label={label} className="inline-flex w-full flex-wrap gap-1 rounded-lg bg-neutral-100 p-1 sm:w-auto">
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={selected}
            onClick={() => onChange(option.value)}
            className={`min-h-10 flex-1 rounded-md px-4 text-sm font-medium transition-colors duration-150 sm:flex-none ${
              selected ? 'bg-white text-neutral-900 shadow-sm' : 'text-neutral-600 hover:text-neutral-900'
            }`}
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
