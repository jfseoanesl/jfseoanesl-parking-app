import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Ellipsis, X } from 'lucide-react'
import type { UserSummary } from '../api/types'
import { mobileNavForRole } from './navigation'
import { NavLinkItem } from './NavLinkItem'

/** Barra inferior movil (09-ux-specification.md §1): objetivo tactil minimo 44px (RNF-004). */
export function BottomNav({ role }: { role: UserSummary['role'] }) {
  const { primary, overflow } = mobileNavForRole(role)
  const [showMore, setShowMore] = useState(false)
  const location = useLocation()
  const overflowActive = overflow.some((item) => item.implemented && item.path === location.pathname)

  // Cerrar el panel "Mas" con Esc.
  useEffect(() => {
    if (!showMore) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setShowMore(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [showMore])

  return (
    <div className="md:hidden print:hidden">
      {showMore && overflow.length > 0 && (
        <>
          <button
            type="button"
            aria-label="Cerrar menú"
            onClick={() => setShowMore(false)}
            className="fixed inset-0 z-20 animate-fade-in bg-neutral-900/40"
          />
          <div
            id="more-menu"
            className="fixed inset-x-0 bottom-16 z-30 animate-toast-in rounded-t-2xl border-t border-neutral-200 bg-white p-4 pb-[calc(1rem+env(safe-area-inset-bottom,0px))] shadow-xl"
          >
            <div className="mb-3 flex items-center justify-between">
              <p className="section-title">Más opciones</p>
              <button
                type="button"
                onClick={() => setShowMore(false)}
                aria-label="Cerrar menú"
                className="flex size-11 items-center justify-center rounded-lg text-neutral-500 hover:bg-neutral-100"
              >
                <X aria-hidden className="size-5" />
              </button>
            </div>
            <ul className="grid grid-cols-2 gap-2">
              {overflow.map((item) => (
                <li key={`${item.label}-${item.path}`}>
                  <NavLinkItem
                    item={item}
                    variant="sheet"
                    onNavigate={() => setShowMore(false)}
                    className="flex min-h-12 items-center gap-3 rounded-lg border border-neutral-200 px-3 text-sm"
                  />
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
      <nav
        aria-label="Navegación principal"
        className="fixed inset-x-0 bottom-0 z-30 flex h-16 border-t border-neutral-200 bg-white/95 pb-[env(safe-area-inset-bottom,0px)] backdrop-blur"
      >
        {primary.map((item) => (
          <div key={`${item.label}-${item.path}`} className="flex flex-1">
            <NavLinkItem
              item={item}
              variant="bottom"
              className="flex min-h-11 flex-1 flex-col items-center justify-center gap-0.5 px-1 text-center text-[11px] leading-tight"
            />
          </div>
        ))}
        {overflow.length > 0 && (
          <button
            type="button"
            onClick={() => setShowMore((v) => !v)}
            aria-expanded={showMore}
            aria-controls="more-menu"
            className={`flex min-h-11 flex-1 flex-col items-center justify-center gap-0.5 px-1 text-center text-[11px] leading-tight transition-colors ${
              showMore || overflowActive ? 'font-semibold text-primary-600' : 'text-neutral-500 hover:text-neutral-900'
            }`}
          >
            <Ellipsis aria-hidden className="size-6" />
            Más
          </button>
        )}
      </nav>
    </div>
  )
}
