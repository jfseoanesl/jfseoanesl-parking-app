/**
 * Placas únicas con formato válido de Colombia (BR-036) para las pruebas E2E: las letras cambian cada
 * segundo y los números con los milisegundos, así que dos ejecuciones no chocan con un parqueo activo.
 */
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'

function letters(seed: number, count: number): string {
  let result = ''
  for (let i = 0; i < count; i++) {
    result += LETTERS[seed % 26]
    seed = Math.floor(seed / 26)
  }
  return result
}

/** Automóvil: ABC123. */
export function carPlate(): string {
  const now = Date.now()
  return letters(Math.floor(now / 1000), 3) + String(now % 1000).padStart(3, '0')
}

/** Motocicleta: ABC12D. */
export function motorcyclePlate(): string {
  const now = Date.now()
  return letters(Math.floor(now / 1000), 3) + String(now % 100).padStart(2, '0') + letters(Math.floor(now / 100), 1)
}
