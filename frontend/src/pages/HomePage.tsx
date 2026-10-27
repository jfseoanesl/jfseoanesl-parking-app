import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Banknote, CircleArrowDown, CircleArrowUp, CircleMinus, CirclePlus, HandCoins, Inbox, SquareParking, Wallet } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { api, ApiError } from '../api/client'
import type { DashboardSummary } from '../api/types'
import { formatCurrency } from '../lib/format'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { Card, PageHeader, StatCard } from '../components/Layout'
import { Badge, PlateBadge } from '../components/Badges'

/** RF-064/RF-065 (Administrador) — wireframe 09-ux-specification.md §5.2. */
export default function HomePage() {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? <AdminDashboard /> : <EmployeeHome />
}

function greeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Buenos días'
  if (hour < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

function EmployeeHome() {
  const { user } = useAuth()
  const firstName = user?.fullName.split(' ')[0] ?? ''
  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={`${greeting()}, ${firstName}`} description="¿Qué quieres hacer ahora?" />
      <div className="grid gap-3 lg:grid-cols-3">
        <QuickAction
          to="/ingreso"
          icon={<CirclePlus aria-hidden className="size-6" />}
          title="Nuevo ingreso"
          description="Registrar un vehículo que llega"
          primary
        />
        <QuickAction
          to="/parqueados"
          icon={<SquareParking aria-hidden className="size-6" />}
          title="Parqueados"
          description="Buscar una placa y cobrar la salida"
        />
        <QuickAction
          to="/caja"
          icon={<Wallet aria-hidden className="size-6" />}
          title="Caja"
          description="Abrir, revisar o cerrar tu caja"
        />
      </div>
    </div>
  )
}

function QuickAction({ to, icon, title, description, primary = false }: {
  to: string
  icon: ReactNode
  title: string
  description: string
  primary?: boolean
}) {
  return (
    <Link
      to={to}
      className={`group flex flex-col gap-3 rounded-xl border p-5 shadow-sm transition-colors duration-150 ${
        primary
          ? 'border-primary-600 bg-primary-600 text-white hover:bg-primary-700'
          : 'border-neutral-200 bg-white text-neutral-900 hover:border-primary-200 hover:bg-primary-50'
      }`}
    >
      <span
        className={`flex size-11 items-center justify-center rounded-lg ${
          primary ? 'bg-white/15 text-white' : 'bg-primary-50 text-primary-600'
        }`}
      >
        {icon}
      </span>
      <span>
        <span className="flex items-center justify-between gap-2 font-semibold">
          {title}
          <ArrowRight aria-hidden className="size-4 shrink-0 transition-transform duration-150 group-hover:translate-x-0.5" />
        </span>
        <span className={`mt-1 block text-sm ${primary ? 'text-primary-100' : 'text-neutral-600'}`}>{description}</span>
      </span>
    </Link>
  )
}

/** Medidor (dataviz): el relleno lleva la severidad y la pista es un paso claro de la misma rampa. */
function occupancyTone(pct: number): { bar: string; track: string; label: string; badge: 'success' | 'warning' | 'danger' } {
  if (pct >= 100) return { bar: 'bg-danger-600', track: 'bg-danger-100', label: 'Lleno', badge: 'danger' }
  if (pct >= 90) return { bar: 'bg-danger-600', track: 'bg-danger-100', label: 'Casi lleno', badge: 'danger' }
  if (pct >= 70) return { bar: 'bg-warning-500', track: 'bg-warning-100', label: 'Ocupación alta', badge: 'warning' }
  return { bar: 'bg-success-600', track: 'bg-success-100', label: 'Disponible', badge: 'success' }
}

function AdminDashboard() {
  const [data, setData] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  function load() {
    api
      .get<DashboardSummary>('/dashboard')
      .then((d) => {
        setData(d)
        setError(null)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudo cargar el dashboard'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    const interval = setInterval(load, 30000)
    return () => clearInterval(interval)
  }, [])

  const header = <PageHeader title="Dashboard" description="Actividad de hoy · se actualiza cada 30 segundos" />

  if (loading) {
    return (
      <div>
        {header}
        <Skeleton className="mb-4 h-32" />
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      </div>
    )
  }
  if (error || !data) {
    return (
      <div>
        {header}
        <Alert>{error}</Alert>
      </div>
    )
  }

  const occupancyPct = data.maxCapacity ? Math.round((data.occupied / data.maxCapacity) * 100) : null
  const tone = occupancyPct != null ? occupancyTone(occupancyPct) : null

  return (
    <div>
      {header}

      <div className="mb-4 grid gap-4 lg:grid-cols-3">
        <div className="card p-5 lg:col-span-2">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-sm text-neutral-600">Ocupación actual</p>
              <p className="mt-1 text-5xl font-semibold tracking-tight text-neutral-900">
                {data.occupied}
                {data.maxCapacity != null && (
                  <span className="text-xl font-normal text-neutral-500"> / {data.maxCapacity}</span>
                )}
              </p>
            </div>
            {tone && <Badge tone={tone.badge}>{tone.label}</Badge>}
          </div>
          {occupancyPct != null && tone ? (
            <>
              <div
                role="progressbar"
                aria-label="Ocupación del parqueadero"
                aria-valuenow={occupancyPct}
                aria-valuemin={0}
                aria-valuemax={100}
                className={`mt-4 h-3 w-full overflow-hidden rounded-full ${tone.track}`}
              >
                <div
                  className={`h-full rounded-full transition-[width] duration-300 ${tone.bar}`}
                  style={{ width: `${Math.min(100, occupancyPct)}%` }}
                />
              </div>
              <p className="mt-2 text-sm text-neutral-600">
                {occupancyPct}% ocupado · {Math.max(0, (data.maxCapacity ?? 0) - data.occupied)} espacios libres
              </p>
            </>
          ) : (
            <p className="mt-2 text-sm text-neutral-500">Configura la capacidad máxima para ver la ocupación.</p>
          )}
        </div>

        <StatCard
          label="Recaudo hoy"
          value={formatCurrency(data.collectedToday)}
          icon={<Banknote aria-hidden className="size-5" />}
          tone="success"
        />
      </div>

      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-3">
        <StatCard label="Ingresos hoy" value={data.entriesToday} icon={<CircleArrowDown aria-hidden className="size-5" />} />
        <StatCard label="Salidas hoy" value={data.exitsToday} icon={<CircleArrowUp aria-hidden className="size-5" />} tone="neutral" />
        <StatCard label="Vehículos parqueados" value={data.occupied} icon={<SquareParking aria-hidden className="size-5" />} />
        <StatCard
          label="Cajas abiertas"
          value={data.openCashSessions}
          icon={<Wallet aria-hidden className="size-5" />}
          tone={data.openCashSessions > 0 ? 'success' : 'warning'}
        />
        {/* RF-084: los otros ingresos ya están incluidos en el recaudo de hoy. */}
        <StatCard label="Otros ingresos hoy" value={formatCurrency(data.otherIncomeToday)} icon={<HandCoins aria-hidden className="size-5" />} tone="neutral" />
        <StatCard
          label="Gastos de caja hoy"
          value={formatCurrency(data.expensesToday)}
          icon={<CircleMinus aria-hidden className="size-5" />}
          tone={data.expensesToday > 0 ? 'warning' : 'neutral'}
        />
      </div>

      <Card title="Últimos movimientos" bodyClassName="">
        {data.recentMovements.length === 0 ? (
          <div className="p-5">
            <EmptyState icon={<Inbox aria-hidden className="size-6" />} title="Sin movimientos recientes" />
          </div>
        ) : (
          <ul className="divide-y divide-neutral-100">
            {data.recentMovements.map((m, i) => (
              <li key={`${m.plate}-${i}`} className="flex items-center justify-between gap-3 px-5 py-3">
                <PlateBadge plate={m.plate} size="sm" />
                <Badge tone={m.status === 'EN_PARQUEADERO' ? 'success' : m.status === 'ANULADO' ? 'danger' : 'neutral'}>
                  {m.description}
                </Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
