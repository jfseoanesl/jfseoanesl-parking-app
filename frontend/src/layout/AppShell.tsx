import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { KeyRound, LogOut } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { initials } from '../lib/format'
import { BrandMark } from './Brand'
import { Sidebar } from './Sidebar'
import { BottomNav } from './BottomNav'
import { PasswordDialog } from '../components/PasswordDialog'

/** Shell autenticado: navegacion (§1) + cabecera con usuario/cerrar sesion + contenido. */
export function AppShell() {
  const { user, logout } = useAuth()
  const [changingPassword, setChangingPassword] = useState(false)
  if (!user) return null

  const roleLabel = user.role === 'ADMIN' ? 'Administrador' : 'Empleado'

  return (
    <div className="flex min-h-svh flex-col md:flex-row">
      <Sidebar role={user.role} />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 print:hidden items-center justify-between gap-3 border-b border-neutral-200 bg-white/95 px-4 backdrop-blur md:px-8">
          <span className="flex items-center gap-2.5 md:hidden">
            <BrandMark className="size-8" />
            <span className="font-semibold text-neutral-900">Parqueadero</span>
          </span>

          <div className="ml-auto flex items-center gap-3">
            <div className="flex items-center gap-2.5">
              <span
                aria-hidden
                className="flex size-9 items-center justify-center rounded-full bg-primary-100 text-sm font-semibold text-primary-700"
              >
                {initials(user.fullName)}
              </span>
              <span className="hidden text-sm leading-tight sm:block">
                <span className="block font-medium text-neutral-900">{user.fullName}</span>
                <span className="block text-neutral-500">{roleLabel}</span>
              </span>
            </div>
            <button
              type="button"
              onClick={() => setChangingPassword(true)}
              aria-label="Cambiar mi contraseña"
              title="Cambiar mi contraseña"
              className="flex size-11 items-center justify-center rounded-lg text-neutral-600 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <KeyRound aria-hidden className="size-5" />
            </button>
            <button
              type="button"
              onClick={() => logout()}
              aria-label="Cerrar sesión"
              className="flex min-h-11 items-center gap-2 rounded-lg px-3 text-sm font-medium text-neutral-600 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <LogOut aria-hidden className="size-5" />
              <span className="hidden sm:inline">Cerrar sesión</span>
            </button>
          </div>
        </header>

        <main className="flex-1 px-4 pt-6 pb-24 md:px-8 md:pb-10 print:p-0">
          <div className="mx-auto w-full max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>

      <BottomNav role={user.role} />
      <PasswordDialog open={changingPassword} target={null} onClose={() => setChangingPassword(false)} />
    </div>
  )
}
