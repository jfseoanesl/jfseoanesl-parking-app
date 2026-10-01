/** Marca de la app: "P" de parqueadero sobre el azul primario (misma forma que el icono PWA). */
export function BrandMark({ className = 'size-9' }: { className?: string }) {
  return (
    <span
      aria-hidden
      className={`flex shrink-0 items-center justify-center rounded-lg bg-primary-600 font-mono text-lg font-bold text-white ${className}`}
    >
      P
    </span>
  )
}

export function Brand({ inverted = false }: { inverted?: boolean }) {
  return (
    <span className="flex items-center gap-2.5">
      <BrandMark />
      <span className={`text-lg font-semibold tracking-tight ${inverted ? 'text-white' : 'text-neutral-900'}`}>
        Parqueadero
      </span>
    </span>
  )
}
