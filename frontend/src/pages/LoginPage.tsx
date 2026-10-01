import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { LockKeyhole, User } from 'lucide-react'
import { useAuth, ApiError } from '../auth/AuthContext'
import { Button } from '../components/Button'
import { Alert } from '../components/Feedback'
import { TextField } from '../components/Form'
import { BrandMark } from '../layout/Brand'

/** UC-001. Wireframe: 09-ux-specification.md §5.1. */
export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    const redirectTo = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={redirectTo} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      // RF-002: mensaje generico tanto si el usuario no existe como si la contraseña es incorrecta.
      setError(err instanceof ApiError ? err.message : 'No se pudo iniciar sesión')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-svh items-center justify-center bg-neutral-900 bg-[radial-gradient(ellipse_at_top,rgb(37_99_235/0.35),transparent_60%)] px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <BrandMark className="size-14 text-2xl" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight text-white">Parqueadero</h1>
          <p className="mt-1 text-sm text-neutral-400">Inicia sesión para comenzar tu turno</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4 p-6 shadow-xl">
          <TextField
            id="username"
            name="username"
            label="Usuario"
            autoComplete="username"
            autoFocus
            adornment={<User aria-hidden className="size-4" />}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <TextField
            id="password"
            name="password"
            type="password"
            label="Contraseña"
            autoComplete="current-password"
            adornment={<LockKeyhole aria-hidden className="size-4" />}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error && <Alert>{error}</Alert>}

          <Button type="submit" size="lg" fullWidth loading={submitting}>
            {submitting ? 'Ingresando…' : 'Iniciar sesión'}
          </Button>
        </form>
      </div>
    </div>
  )
}
