import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from './client'

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
  })

  it('traduce un fallo de red a un ApiError con mensaje claro de conexion (RNF-007)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(api.get('/dashboard')).rejects.toMatchObject({
      message: 'Sin conexión a internet. Verifica tu conexión e inténtalo de nuevo.',
      code: 'NETWORK_ERROR',
    })
    await expect(api.get('/dashboard')).rejects.toBeInstanceOf(ApiError)
  })

  it('propaga el mensaje del servidor cuando la respuesta no es exitosa', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: 'NOT_FOUND', message: 'No existe' }), {
          status: 404,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    await expect(api.get('/parking-sessions/x')).rejects.toMatchObject({
      status: 404,
      code: 'NOT_FOUND',
      message: 'No existe',
    })
  })

  it('envia el header X-XSRF-TOKEN en mutaciones cuando existe la cookie', async () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/;'
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await api.post('/cash-sessions/open', { initialBalance: 1000 })

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Headers
    expect(headers.get('X-XSRF-TOKEN')).toBe('abc123')
  })

  it('download devuelve el archivo con el nombre del header Content-Disposition (RF-063)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('%PDF-1.4', {
          status: 200,
          headers: {
            'Content-Type': 'application/pdf',
            'Content-Disposition': "attachment; filename=\"reporte.pdf\"; filename*=UTF-8''reporte-parqueadero_2026-09-29.pdf",
          },
        }),
      ),
    )

    const { blob, filename } = await api.download('/reports/summary/export?format=pdf', 'respaldo.pdf')

    expect(filename).toBe('reporte-parqueadero_2026-09-29.pdf')
    expect(await blob.text()).toBe('%PDF-1.4')
  })

  it('download traduce un error del servidor a ApiError con su mensaje', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: 'BAD_REQUEST', message: 'Formato no soportado: use xlsx o pdf' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    await expect(api.download('/reports/summary/export?format=docx', 'x')).rejects.toMatchObject({
      status: 400,
      message: 'Formato no soportado: use xlsx o pdf',
    })
  })

  it('obtiene la cookie CSRF antes de una mutacion si aun no existe (login en la primera visita)', async () => {
    const fetchMock = vi.fn((url: string) => {
      // El GET de cebado simula la respuesta del servidor que crea la cookie.
      if (url === '/api/auth/me') document.cookie = 'XSRF-TOKEN=fresh-token; path=/;'
      return Promise.resolve(new Response(null, { status: url === '/api/auth/me' ? 401 : 204 }))
    })
    vi.stubGlobal('fetch', fetchMock)

    await api.post('/auth/login', { username: 'a', password: 'b' })

    expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/me')
    const [loginUrl, init] = fetchMock.mock.calls[1] as unknown as [string, RequestInit]
    expect(loginUrl).toBe('/api/auth/login')
    expect((init.headers as Headers).get('X-XSRF-TOKEN')).toBe('fresh-token')
  })
})
