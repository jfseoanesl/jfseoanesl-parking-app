import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { CircleAlert, CircleCheck, X } from 'lucide-react'

type ToastTone = 'success' | 'danger'

interface ToastItem {
  id: number
  tone: ToastTone
  message: string
}

const ToastContext = createContext<((message: string, tone?: ToastTone) => void) | null>(null)

const AUTO_DISMISS_MS = 4000

/**
 * Confirmacion visual breve y no bloqueante (09-ux-specification.md §4 Toast): se cierra sola
 * (4s) y se anuncia a lectores de pantalla via aria-live, sin mover el foco del usuario.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(1)

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((t) => t.id !== id))
  }, [])

  const show = useCallback(
    (message: string, tone: ToastTone = 'success') => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, tone, message }])
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-20 z-50 flex flex-col items-center gap-2 px-4 md:bottom-6 md:items-end md:px-6 print:hidden"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className="pointer-events-auto flex w-full max-w-sm animate-toast-in items-center gap-3 rounded-xl bg-neutral-900 px-4 py-3 text-sm text-white shadow-lg"
          >
            {toast.tone === 'success' ? (
              <CircleCheck aria-hidden className="size-5 shrink-0 text-success-500" />
            ) : (
              <CircleAlert aria-hidden className="size-5 shrink-0 text-danger-500" />
            )}
            <span className="flex-1">{toast.message}</span>
            <button
              type="button"
              onClick={() => dismiss(toast.id)}
              aria-label="Cerrar aviso"
              className="-mr-1 flex size-8 items-center justify-center rounded-md text-neutral-400 transition-colors hover:bg-white/10 hover:text-white"
            >
              <X aria-hidden className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const show = useContext(ToastContext)
  if (!show) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return show
}
