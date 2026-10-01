import { useEffect, useState, type FormEvent } from 'react'
import { KeyRound, Plus, Power, ShieldCheck, User, Users, X } from 'lucide-react'
import { api, ApiError } from '../api/client'
import type { AdminUser } from '../api/types'
import { initials } from '../lib/format'
import { Button } from '../components/Button'
import { Alert, EmptyState, Skeleton } from '../components/Feedback'
import { ChoiceGroup, TextField } from '../components/Form'
import { Card, PageHeader } from '../components/Layout'
import { Badge } from '../components/Badges'
import { useToast } from '../components/Toast'
import { PasswordDialog } from '../components/PasswordDialog'

type Role = 'EMPLOYEE' | 'ADMIN'

/** UC-009 (RF-005 a RF-009). Wireframe: 09-ux-specification.md §5.10. */
export default function UsersPage() {
  const toast = useToast()
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

  const [username, setUsername] = useState('')
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('EMPLOYEE')
  const [submitting, setSubmitting] = useState(false)
  const [passwordTarget, setPasswordTarget] = useState<AdminUser | null>(null)

  function load() {
    api
      .get<AdminUser[]>('/users')
      .then(setUsers)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'No se pudieron cargar los usuarios'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  function closeForm() {
    setUsername('')
    setFullName('')
    setPassword('')
    setRole('EMPLOYEE')
    setError(null)
    setShowForm(false)
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.post('/users', { username, fullName, password, role })
      toast(`Usuario "${username}" creado`)
      closeForm()
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear el usuario')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Usuarios"
        description="Personas con acceso a la aplicación y su rol."
        actions={
          !showForm && (
            <Button icon={<Plus aria-hidden className="size-5" />} onClick={() => setShowForm(true)}>
              Nuevo usuario
            </Button>
          )
        }
      />

      {showForm && (
        <Card
          className="mb-6"
          title="Nuevo usuario"
          actions={
            <button
              type="button"
              aria-label="Cerrar formulario"
              onClick={closeForm}
              className="flex size-11 items-center justify-center rounded-lg text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-900"
            >
              <X aria-hidden className="size-5" />
            </button>
          }
        >
          <form onSubmit={handleCreate} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField label="Nombre completo" value={fullName} onChange={(e) => setFullName(e.target.value)} autoComplete="off" required />
              <TextField label="Usuario" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="off" required />
            </div>
            <TextField
              label="Contraseña inicial"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              hint="Mínimo 8 caracteres. La persona podrá cambiarla después."
              required
            />
            <ChoiceGroup<Role>
              label="Rol"
              value={role}
              onChange={setRole}
              options={[
                { value: 'EMPLOYEE', label: 'Empleado', description: 'Ingresos, cobros y su caja', icon: <User aria-hidden className="size-5" /> },
                { value: 'ADMIN', label: 'Administrador', description: 'Acceso completo', icon: <ShieldCheck aria-hidden className="size-5" /> },
              ]}
            />
            {error && <Alert>{error}</Alert>}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button variant="secondary" onClick={closeForm}>
                Cancelar
              </Button>
              <Button type="submit" loading={submitting}>
                Crear usuario
              </Button>
            </div>
          </form>
        </Card>
      )}

      {!showForm && error && <Alert className="mb-4">{error}</Alert>}

      {loading ? (
        <div className="flex flex-col gap-2">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      ) : users.length === 0 ? (
        <EmptyState icon={<Users aria-hidden className="size-6" />} title="No hay usuarios" />
      ) : (
        <ul className="card divide-y divide-neutral-100">
          {users.map((user) => (
            <li key={user.id} className="flex flex-wrap items-center gap-3 px-5 py-4">
              <span
                aria-hidden
                className={`flex size-10 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${
                  user.active ? 'bg-primary-100 text-primary-700' : 'bg-neutral-100 text-neutral-500'
                }`}
              >
                {initials(user.fullName)}
              </span>
              <div className="min-w-0 flex-1">
                <p className={`truncate font-medium ${user.active ? 'text-neutral-900' : 'text-neutral-500'}`}>{user.fullName}</p>
                <p className="truncate font-mono text-sm text-neutral-500">{user.username}</p>
              </div>
              <div className="flex items-center gap-2">
                <Badge tone={user.role === 'ADMIN' ? 'primary' : 'neutral'} dot={false}>
                  {user.role === 'ADMIN' ? 'Administrador' : 'Empleado'}
                </Badge>
                <Badge tone={user.active ? 'success' : 'danger'}>{user.active ? 'Activo' : 'Inactivo'}</Badge>
              </div>
              <div className="ml-auto flex items-center gap-1 sm:ml-0">
                <Button variant="ghost" onClick={() => setPasswordTarget(user)} icon={<KeyRound aria-hidden className="size-4" />}>
                  Contraseña
                </Button>
                <ActiveToggleButton user={user} onChanged={load} />
              </div>
            </li>
          ))}
        </ul>
      )}

      <PasswordDialog open={passwordTarget !== null} target={passwordTarget} onClose={() => setPasswordTarget(null)} />
    </div>
  )
}

function ActiveToggleButton({ user, onChanged }: { user: AdminUser; onChanged: () => void }) {
  const toast = useToast()
  const [submitting, setSubmitting] = useState(false)

  async function toggle() {
    setSubmitting(true)
    try {
      await api.patch(`/users/${user.id}/status`, { active: !user.active })
      toast(`${user.fullName} ${user.active ? 'desactivado' : 'activado'}`)
      onChanged()
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado', 'danger')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Button variant="ghost" onClick={toggle} loading={submitting} icon={<Power aria-hidden className="size-4" />} >
      {user.active ? 'Desactivar' : 'Activar'}
    </Button>
  )
}
