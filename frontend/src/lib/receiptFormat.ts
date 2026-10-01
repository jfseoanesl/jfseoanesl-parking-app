import type { ReceiptFormat } from '../api/types'

/** RF-015: por defecto el comprobante muestra todo, sin pie de página. */
export const DEFAULT_RECEIPT_FORMAT: ReceiptFormat = {
  showNit: true,
  showAddress: true,
  showPhone: true,
  showEmployee: true,
  showTariff: true,
  showDuration: true,
  footer: '',
}

/** Lee Configuration.receiptFormatJson; un valor ausente o inválido cae en los valores por defecto. */
export function parseReceiptFormat(json: string | null | undefined): ReceiptFormat {
  if (!json) return DEFAULT_RECEIPT_FORMAT
  try {
    const parsed = JSON.parse(json) as Partial<ReceiptFormat>
    return {
      showNit: parsed.showNit ?? true,
      showAddress: parsed.showAddress ?? true,
      showPhone: parsed.showPhone ?? true,
      showEmployee: parsed.showEmployee ?? true,
      showTariff: parsed.showTariff ?? true,
      showDuration: parsed.showDuration ?? true,
      footer: typeof parsed.footer === 'string' ? parsed.footer : '',
    }
  } catch {
    return DEFAULT_RECEIPT_FORMAT
  }
}
