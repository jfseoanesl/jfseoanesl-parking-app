/**
 * Cliente HTTP minimo para la API (08-api-contract.md). Maneja el token CSRF de doble envio
 * (decisions/ADR-004-autenticacion.md): Spring Security expone la cookie XSRF-TOKEN en
 * cualquier GET; este cliente la lee y la reenvia como header en cada mutacion.
 */

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.split('=')[1]) : null
}

let csrfPriming: Promise<unknown> | null = null

/**
 * Garantiza que exista la cookie XSRF-TOKEN antes de una mutacion. Sin esto, una mutacion enviada
 * antes de que termine el primer GET de la pagina (ej. un login muy rapido en la primera visita)
 * salia sin token y el servidor respondia 403. Cualquier GET entrega la cookie, incluso con 401.
 */
async function ensureCsrfCookie(): Promise<void> {
  if (readCookie('XSRF-TOKEN')) return
  csrfPriming ??= fetch('/api/auth/me', { credentials: 'include' })
    .catch(() => undefined)
    .finally(() => {
      csrfPriming = null
    })
  await csrfPriming
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (method !== 'GET' && method !== 'HEAD') {
    await ensureCsrfCookie()
    const csrfToken = readCookie('XSRF-TOKEN')
    if (csrfToken) headers.set('X-XSRF-TOKEN', csrfToken)
    if (init.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
  }

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      method,
      headers,
      credentials: 'include',
    })
  } catch {
    // RNF-007: la operacion es transaccional y requiere red; el shell estatico (PWA) sigue
    // disponible offline, pero esta llamada no puede completarse sin conexion.
    throw new ApiError(0, 'NETWORK_ERROR', 'Sin conexión a internet. Verifica tu conexión e inténtalo de nuevo.')
  }

  if (response.status === 204) return undefined as T

  const isJson = response.headers.get('content-type')?.includes('application/json')
  const body = isJson ? await response.json() : undefined

  if (!response.ok) {
    throw new ApiError(
      response.status,
      body?.error ?? 'UNKNOWN_ERROR',
      body?.message ?? 'Ocurrió un error inesperado',
    )
  }

  return body as T
}

/** Nombre de archivo del header Content-Disposition (filename* UTF-8 o filename simple). */
function filenameFrom(disposition: string | null, fallback: string): string {
  if (!disposition) return fallback
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded) return decodeURIComponent(encoded[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : fallback
}

/** Descarga binaria (ej. reportes exportados, RF-063): mismo manejo de errores que request(). */
async function download(path: string, fallbackName: string): Promise<{ blob: Blob; filename: string }> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, { credentials: 'include' })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Sin conexión a internet. Verifica tu conexión e inténtalo de nuevo.')
  }
  if (!response.ok) {
    const isJson = response.headers.get('content-type')?.includes('application/json')
    const body = isJson ? await response.json() : undefined
    throw new ApiError(response.status, body?.error ?? 'UNKNOWN_ERROR', body?.message ?? 'No se pudo descargar el archivo')
  }
  return {
    blob: await response.blob(),
    filename: filenameFrom(response.headers.get('content-disposition'), fallbackName),
  }
}

export const api = {
  download,
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
}
