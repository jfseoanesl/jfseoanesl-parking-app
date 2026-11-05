/**
 * BR-036: formato de placa de Colombia por tipo de vehículo, igual que PlateFormat del backend.
 * Automóvil (CAR): ABC123. Motocicleta (MOTORCYCLE): ABC12D o la serie antigua ABC12. Los tipos con
 * otro código no tienen formato oficial y solo pasan la validación laxa (BR-002).
 */
export type FormattedTypeCode = 'CAR' | 'MOTORCYCLE'

const CAR = /^[A-Z]{3}\d{3}$/
const MOTORCYCLE = /^[A-Z]{3}\d{2}[A-Z]?$/

const LABELS: Record<FormattedTypeCode, string> = { CAR: 'automóvil', MOTORCYCLE: 'motocicleta' }
const EXAMPLES: Record<FormattedTypeCode, string> = { CAR: 'ABC123', MOTORCYCLE: 'ABC12D o ABC12' }

function isFormatted(code: string | null | undefined): code is FormattedTypeCode {
  return code === 'CAR' || code === 'MOTORCYCLE'
}

/** Tipo cuyo formato coincide con la placa (ya normalizada), si alguno. */
export function detectPlateType(plate: string): FormattedTypeCode | null {
  if (CAR.test(plate)) return 'CAR'
  if (MOTORCYCLE.test(plate)) return 'MOTORCYCLE'
  return null
}

export interface PlateFormatIssue {
  message: string
  /** Tipo al que corresponde la placa, para ofrecer el cambio. */
  suggested: FormattedTypeCode | null
}

/** null si la placa es válida para el tipo; si no, el aviso (mismo texto que el backend). */
export function plateFormatIssue(plate: string, typeCode: string | null | undefined): PlateFormatIssue | null {
  if (!plate || !isFormatted(typeCode)) return null
  const detected = detectPlateType(plate)
  if (detected === typeCode) return null
  if (detected) {
    return {
      message: `La placa ${plate} tiene formato de ${LABELS[detected]}, no de ${LABELS[typeCode]}`,
      suggested: detected,
    }
  }
  return { message: `La placa ${plate} no tiene el formato de ${LABELS[typeCode]} (${EXAMPLES[typeCode]})`, suggested: null }
}
