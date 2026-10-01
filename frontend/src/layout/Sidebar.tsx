import type { UserSummary } from '../api/types'
import { Brand } from './Brand'
import { navForRole } from './navigation'
import { NavLinkItem } from './NavLinkItem'

/** Navegacion de escritorio/tablet ancho (09-ux-specification.md §1). */
export function Sidebar({ role }: { role: UserSummary['role'] }) {
  return (
    <nav
      aria-label="Navegación principal"
      className="hidden w-64 shrink-0 bg-neutral-900 md:block print:hidden"
    >
      <div className="sticky top-0 flex h-svh flex-col px-3 py-5">
        <div className="mb-8 px-3">
          <Brand inverted />
        </div>
        <ul className="flex flex-col gap-1">
          {navForRole(role).map((item) => (
            <li key={`${item.label}-${item.path}`}>
              <NavLinkItem
                item={item}
                variant="sidebar"
                className="flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm"
              />
            </li>
          ))}
        </ul>
        <p className="mt-auto px-3 text-xs text-neutral-500">
          {role === 'ADMIN' ? 'Panel de administración' : 'Operación de turno'}
        </p>
      </div>
    </nav>
  )
}
