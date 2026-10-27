import { useEffect, useState, type FormEvent } from 'react'
import { Check, Pencil, Plus, X } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { CashConcept, CashTransactionType } from '../api/types'
import { Button } from './Button'
import { Alert, Skeleton } from './Feedback'
import { SegmentedControl, TextField } from './Form'
import { Card } from './Layout'
import { useToast } from './Toast'

const TYPE_LABELS: Record<CashTransactionType, string> = { INCOME: 'Otros ingresos', EXPENSE: 'Gastos' }

/** RF-079: catálogo de conceptos de otros ingresos y gastos de caja (solo administrador). */
export function CashConceptCatalog() {
  const toast = useToast()
  const [type, setType] = useState<CashTransactionType>('INCOME')
  const [concepts, setConcepts] = useState<CashConcept[] | null>(null)
  const [name, setName] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingName, setEditingName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function load() {
    api
      .get<CashConcept[]>('/cash-concepts')
      .then(setConcepts)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los conceptos'))
  }

  useEffect(load, [])

  async function run(action: () => Promise<unknown>, message: string) {
    setError(null)
    setSubmitting(true)
    try {
      await action()
      toast(message)
      load()
      return true
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el concepto')
      return false
    } finally {
      setSubmitting(false)
    }
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault()
    const clean = name.trim()
    if (await run(() => api.post('/cash-concepts', { type, name: clean }), `Concepto "${clean}" agregado`)) setName('')
  }

  async function saveRename(concept: CashConcept) {
    const clean = editingName.trim()
    if (await run(() => api.put(`/cash-concepts/${concept.id}`, { name: clean }), 'Concepto actualizado')) setEditingId(null)
  }

  const visible = (concepts ?? []).filter((c) => c.type === type)

  return (
    <Card title="Conceptos de caja" description="Conceptos para registrar otros ingresos y gastos en caja.">
      <SegmentedControl
        label="Tipo de concepto"
        options={(Object.keys(TYPE_LABELS) as CashTransactionType[]).map((t) => ({ value: t, label: TYPE_LABELS[t] }))}
        value={type}
        onChange={setType}
      />

      {concepts === null ? (
        <Skeleton className="mt-4 h-16" />
      ) : (
        <ul className="mt-4 divide-y divide-neutral-100">
          {visible.length === 0 && <li className="py-2 text-sm text-neutral-500">Sin conceptos de este tipo.</li>}
          {visible.map((c) => (
            <li key={c.id} className="flex flex-wrap items-center gap-2 py-2">
              {editingId === c.id ? (
                <>
                  <input
                    aria-label="Nuevo nombre"
                    className="input min-w-0 flex-1"
                    value={editingName}
                    maxLength={100}
                    onChange={(e) => setEditingName(e.target.value)}
                  />
                  <Button variant="ghost" aria-label="Guardar nombre" icon={<Check aria-hidden className="size-4" />} loading={submitting} onClick={() => saveRename(c)} />
                  <Button variant="ghost" aria-label="Cancelar" icon={<X aria-hidden className="size-4" />} onClick={() => setEditingId(null)} />
                </>
              ) : (
                <>
                  <span className={`min-w-0 flex-1 text-sm ${c.active ? 'text-neutral-800' : 'text-neutral-400 line-through'}`}>{c.name}</span>
                  <Button
                    variant="ghost"
                    aria-label={`Editar ${c.name}`}
                    icon={<Pencil aria-hidden className="size-4" />}
                    onClick={() => {
                      setEditingId(c.id)
                      setEditingName(c.name)
                    }}
                  />
                  <Button
                    variant="ghost"
                    disabled={submitting}
                    onClick={() =>
                      run(() => api.patch(`/cash-concepts/${c.id}/status`, { active: !c.active }), c.active ? 'Concepto desactivado' : 'Concepto activado')
                    }
                  >
                    {c.active ? 'Desactivar' : 'Activar'}
                  </Button>
                </>
              )}
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={handleAdd} className="mt-4 space-y-3">
        <TextField
          label={type === 'INCOME' ? 'Nuevo concepto de ingreso' : 'Nuevo concepto de gasto'}
          value={name}
          maxLength={100}
          onChange={(e) => setName(e.target.value)}
          placeholder={type === 'INCOME' ? 'Ej. Lavado' : 'Ej. Papelería'}
          required
        />
        {error && <Alert>{error}</Alert>}
        <Button type="submit" variant="secondary" fullWidth loading={submitting} icon={<Plus aria-hidden className="size-4" />}>
          Agregar concepto
        </Button>
      </form>
    </Card>
  )
}
