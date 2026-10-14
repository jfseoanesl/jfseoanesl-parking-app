import type { ReactNode } from 'react'
import {
  ChartColumn,
  DatabaseBackup,
  CirclePlus,
  History,
  House,
  LayoutDashboard,
  ReceiptText,
  Settings,
  ShieldCheck,
  SquareParking,
  Tags,
  Users,
  Wallet,
} from 'lucide-react'

/**
 * Iconografia de navegacion (09-ux-specification.md §1): simple y reconocible. Se indexa por
 * label (no por path) porque "Parqueados" y "Cobros" comparten ruta pero no icono.
 */
const ICONS: Record<string, (className: string) => ReactNode> = {
  Inicio: (c) => <House aria-hidden className={c} />,
  Dashboard: (c) => <LayoutDashboard aria-hidden className={c} />,
  'Nuevo ingreso': (c) => <CirclePlus aria-hidden className={c} />,
  Parqueados: (c) => <SquareParking aria-hidden className={c} />,
  Cobros: (c) => <ReceiptText aria-hidden className={c} />,
  Caja: (c) => <Wallet aria-hidden className={c} />,
  Historial: (c) => <History aria-hidden className={c} />,
  Reportes: (c) => <ChartColumn aria-hidden className={c} />,
  Tarifas: (c) => <Tags aria-hidden className={c} />,
  Usuarios: (c) => <Users aria-hidden className={c} />,
  Configuración: (c) => <Settings aria-hidden className={c} />,
  Auditoría: (c) => <ShieldCheck aria-hidden className={c} />,
  Respaldo: (c) => <DatabaseBackup aria-hidden className={c} />,
}

export function navIcon(label: string, className = 'size-5 shrink-0'): ReactNode {
  return ICONS[label]?.(className) ?? null
}
