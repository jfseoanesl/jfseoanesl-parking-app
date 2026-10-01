import { NavLink } from 'react-router-dom'
import type { NavItem } from './navigation'
import { navIcon } from './navIcons'

type Variant = 'sidebar' | 'bottom' | 'sheet'

const COLORS: Record<Variant, { active: string; inactive: string }> = {
  sidebar: {
    active: 'bg-white/10 text-white font-medium',
    inactive: 'text-neutral-300 hover:bg-white/5 hover:text-white',
  },
  bottom: {
    active: 'text-primary-600 font-semibold',
    inactive: 'text-neutral-500 hover:text-neutral-900',
  },
  sheet: {
    active: 'bg-primary-50 text-primary-700 font-semibold',
    inactive: 'text-neutral-700 hover:bg-neutral-100',
  },
}

/** Item de navegacion compartido por Sidebar y BottomNav; deshabilitado si aun no existe pantalla. */
export function NavLinkItem({ item, className, variant = 'sidebar', onNavigate }: {
  item: NavItem
  className: string
  variant?: Variant
  onNavigate?: () => void
}) {
  const icon = navIcon(item.label, variant === 'bottom' ? 'size-6 shrink-0' : 'size-5 shrink-0')

  if (!item.implemented) {
    return (
      <span
        aria-disabled="true"
        title="Disponible próximamente"
        className={`${className} cursor-not-allowed opacity-40 ${COLORS[variant].inactive}`}
      >
        {icon}
        {item.label}
      </span>
    )
  }

  return (
    <NavLink
      to={item.path}
      end
      onClick={onNavigate}
      className={({ isActive }) =>
        `${className} transition-colors duration-150 ${isActive ? COLORS[variant].active : COLORS[variant].inactive}`
      }
    >
      {icon}
      {item.label}
    </NavLink>
  )
}
