import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import { Button } from './Button'
import { Alert } from './Feedback'
import { TextField } from './Form'
import { useToast } from './Toast'

/**
 * Cambio de contraseña (UC-009): propia (RF-008, exige la contraseña actual) o de otro usuario
 * por un Administrador (RF-007). Mismo patrón de <dialog> modal que ConfirmDialog.
 */
export function PasswordDialog({
  open,
  target,
  onClose,
}: {
  open: boolean
  /** null = contraseña propia; en otro caso, el usuario administrado. */
  target: { id: string; fullName: string } | null
  onClose: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const toast = useToast()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const own = target === null

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      setCurrentPassword('')
      setNewPassword('')
      setConfirmation('')
      setError(null)
      dialog.showModal()
    }
    if (!open && dialog.open) dialog.close()
  }, [open])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (newPassword !== confirmation) {
      setError('La confirmación no coincide con la nueva contraseña')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      if (own) await api.put('/users/me/password', { currentPassword, newPassword })
      else await api.put(`/users/${target.id}/password`, { newPassword })
      toast(own ? 'Tu contraseña fue actualizada' : `Contraseña de ${target.fullName} actualizada`)
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cambiar la contraseña')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby="password-dialog-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!submitting) onClose()
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-md animate-fade-in rounded-2xl bg-white p-0 shadow-xl"
    >
      <form onSubmit={handleSubmit}>
        <div className="space-y-4 p-6">
          <h2 id="password-dialog-title" className="text-lg font-semibold text-neutral-900">
            {own ? 'Cambiar mi contraseña' : `Cambiar contraseña de ${target.fullName}`}
          </h2>
          {own && (
            <TextField
              label="Contraseña actual"
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          )}
          <TextField
            label="Nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            hint="Mínimo 8 caracteres."
            required
          />
          <TextField
            label="Confirmar nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            minLength={8}
            required
          />
          {error && <Alert>{error}</Alert>}
        </div>
        <div className="flex flex-col-reverse gap-2 border-t border-neutral-100 bg-neutral-50 px-6 py-4 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={submitting}>
            Guardar contraseña
          </Button>
        </div>
      </form>
    </dialog>
  )
}
