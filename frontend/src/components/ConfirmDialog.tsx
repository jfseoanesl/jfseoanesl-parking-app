import { useEffect, useRef, type ReactNode } from 'react'
import { Button } from './Button'

/**
 * Confirmacion explicita para operaciones irreversibles o financieras (09-ux-specification.md §4,
 * §6): cobro, cierre de caja, anulacion. Usa <dialog> nativo en modo modal: atrapa el foco, cierra
 * con Esc y devuelve el foco al elemento que lo abrio.
 */
export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  cancelLabel = 'Cancelar',
  tone = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  cancelLabel?: string
  tone?: 'primary' | 'danger' | 'success'
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby="confirm-dialog-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!loading) onCancel()
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-md animate-fade-in rounded-2xl bg-white p-0 shadow-xl"
    >
      <div className="p-6">
        <h2 id="confirm-dialog-title" className="text-lg font-semibold text-neutral-900">
          {title}
        </h2>
        <div className="mt-3 text-sm text-neutral-600">{children}</div>
      </div>
      <div className="flex flex-col-reverse gap-2 border-t border-neutral-100 bg-neutral-50 px-6 py-4 sm:flex-row sm:justify-end">
        <Button variant="secondary" onClick={onCancel} disabled={loading}>
          {cancelLabel}
        </Button>
        <Button variant={tone} onClick={onConfirm} loading={loading} autoFocus>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  )
}
