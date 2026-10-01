import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { NavLinkItem } from './NavLinkItem'

describe('NavLinkItem', () => {
  it('un item no implementado se muestra deshabilitado y sin enlace navegable', () => {
    render(
      <MemoryRouter>
        <NavLinkItem item={{ label: 'Historial', path: '/historial', implemented: false }} className="" />
      </MemoryRouter>,
    )

    const item = screen.getByText('Historial')
    expect(item.tagName).not.toBe('A')
    expect(item).toHaveAttribute('aria-disabled', 'true')
  })

  it('un item implementado se renderiza como enlace navegable', () => {
    render(
      <MemoryRouter>
        <NavLinkItem item={{ label: 'Reportes', path: '/reportes', implemented: true }} className="" />
      </MemoryRouter>,
    )

    const link = screen.getByRole('link', { name: 'Reportes' })
    expect(link).toHaveAttribute('href', '/reportes')
  })
})
