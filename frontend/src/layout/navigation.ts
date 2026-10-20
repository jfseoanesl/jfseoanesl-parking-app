import type { UserSummary } from '../api/types'

export interface NavItem {
  label: string
  path: string
  /** false = todavia no tiene pantalla implementada (se agrega en Fases 5-8). */
  implemented: boolean
}

/** Mapa de navegacion (09-ux-specification.md §1). */
const EMPLOYEE_NAV: NavItem[] = [
  { label: 'Inicio', path: '/', implemented: true },
  { label: 'Nuevo ingreso', path: '/ingreso', implemented: true },
  { label: 'Parqueados', path: '/parqueados', implemented: true },
  // "Cobros" reutiliza la lista de Parqueados como punto de partida (RF-033: seleccion
  // inmediata desde ahi para registrar salida) - no existe una pantalla "Cobros" separada.
  { label: 'Cobros', path: '/parqueados', implemented: true },
  { label: 'Caja', path: '/caja', implemented: true },
  { label: 'Mensualidades', path: '/mensualidades', implemented: true },
]

const ADMIN_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/', implemented: true },
  { label: 'Parqueados', path: '/parqueados', implemented: true },
  { label: 'Historial', path: '/historial', implemented: true },
  { label: 'Reportes', path: '/reportes', implemented: true },
  { label: 'Tarifas', path: '/tarifas', implemented: true },
  { label: 'Usuarios', path: '/usuarios', implemented: true },
  { label: 'Caja', path: '/caja', implemented: true },
  { label: 'Mensualidades', path: '/mensualidades', implemented: true },
  { label: 'Configuración', path: '/configuracion', implemented: true },
  { label: 'Auditoría', path: '/auditoria', implemented: true },
  { label: 'Respaldo', path: '/respaldo', implemented: true },
]

/** Barra inferior movil del administrador: 4 mas usados + "Mas" (09-ux-specification.md §1). */
const ADMIN_MOBILE_PRIMARY_COUNT = 4

export function navForRole(role: UserSummary['role']): NavItem[] {
  return role === 'ADMIN' ? ADMIN_NAV : EMPLOYEE_NAV
}

export function mobileNavForRole(role: UserSummary['role']): {
  primary: NavItem[]
  overflow: NavItem[]
} {
  const items = navForRole(role)
  // Hasta 5 caben como primarios; con mas, 4 primarios + "Mas" (igual que el administrador).
  if (items.length <= 5) return { primary: items, overflow: [] }

  // Prioriza items ya implementados para los 4 espacios primarios (un item deshabilitado no
  // deberia ocupar un lugar en la barra inferior mientras haya uno usable esperando en "Mas").
  // El orden dentro de cada grupo respeta el orden declarado (prompt.md §21).
  const implemented = items.filter((i) => i.implemented)
  const notImplemented = items.filter((i) => !i.implemented)
  const ordered = [...implemented, ...notImplemented]
  return {
    primary: ordered.slice(0, ADMIN_MOBILE_PRIMARY_COUNT),
    overflow: ordered.slice(ADMIN_MOBILE_PRIMARY_COUNT),
  }
}
