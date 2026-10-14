import { useRef, useState, type ChangeEvent } from 'react'
import { DatabaseBackup, Download, FileUp, TriangleAlert, Upload } from 'lucide-react'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { BACKUP_TABLE_LABELS, parseBackupFile, type BackupSummary } from '../lib/backup'
import { saveBlob } from '../lib/download'
import { formatDateTime } from '../lib/format'
import { Button } from '../components/Button'
import { Alert } from '../components/Feedback'
import { Card, PageHeader } from '../components/Layout'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'

const CONFIRM_WORD = 'REEMPLAZAR'

/**
 * RF-073 / ADR-009: respaldo completo de la base de datos. Exportar descarga un JSON portable
 * (SQLite o MySQL); importar reemplaza todos los datos de esta instalación en una sola transacción.
 */
export default function BackupPage() {
  const toast = useToast()
  const { logout } = useAuth()
  const fileRef = useRef<HTMLInputElement>(null)
  const [exporting, setExporting] = useState(false)
  const [summary, setSummary] = useState<BackupSummary | null>(null)
  const [fileName, setFileName] = useState('')
  const [fileError, setFileError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)
  const [confirmText, setConfirmText] = useState('')
  const [importError, setImportError] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)

  async function handleExport() {
    setExporting(true)
    try {
      const { blob, filename } = await api.download('/admin/backup', 'parking-respaldo.json')
      saveBlob(blob, filename)
      toast(`Respaldo descargado: ${filename}`)
    } catch (err) {
      toast(err instanceof ApiError ? err.message : 'No se pudo generar el respaldo', 'danger')
    } finally {
      setExporting(false)
    }
  }

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    setSummary(null)
    setFileError(null)
    setImportError(null)
    if (!file) return
    setFileName(file.name)
    try {
      setSummary(parseBackupFile(await file.text()))
    } catch (err) {
      setFileError(err instanceof Error ? err.message : 'No se pudo leer el archivo')
    }
  }

  async function handleImport() {
    if (!summary) return
    if (confirmText.trim().toUpperCase() !== CONFIRM_WORD) {
      setImportError(`Escribe ${CONFIRM_WORD} para confirmar`)
      return
    }
    setImporting(true)
    setImportError(null)
    try {
      await api.post('/admin/backup/import', summary.data)
      toast('Respaldo importado. Inicia sesión con un usuario del respaldo.')
      // Los usuarios cambiaron: se cierra la sesión y se recarga para empezar limpio.
      await logout().catch(() => undefined)
      window.location.assign('/login')
    } catch (err) {
      setImportError(err instanceof ApiError ? err.message : 'No se pudo importar el respaldo')
      setImporting(false)
    }
  }

  function resetFile() {
    setSummary(null)
    setFileName('')
    setFileError(null)
    if (fileRef.current) fileRef.current.value = ''
  }

  return (
    <div>
      <PageHeader title="Respaldo" description="Exporta toda la información o restaura un respaldo en esta instalación." />

      <div className="grid gap-4 lg:grid-cols-2 lg:items-start">
        <Card title="Exportar respaldo" description="Descarga un archivo con todos los datos de la aplicación.">
          <div className="space-y-4">
            <div className="flex gap-3 rounded-xl bg-warning-50 p-4 text-sm text-warning-800">
              <TriangleAlert aria-hidden className="size-5 shrink-0 text-warning-600" />
              <p>
                El archivo contiene usuarios (con contraseñas cifradas), cobros, cajas y auditoría. Guárdalo en un
                lugar seguro y no lo compartas.
              </p>
            </div>
            <p className="text-sm text-neutral-600">
              Sirve para restaurar esta instalación o para pasar los datos a otra (también de SQLite a MySQL), siempre
              que tenga la misma versión de la aplicación.
            </p>
            <Button icon={<Download aria-hidden className="size-5" />} loading={exporting} onClick={handleExport}>
              Descargar respaldo
            </Button>
          </div>
        </Card>

        <Card title="Importar respaldo" description="Reemplaza todos los datos actuales por los del archivo.">
          <div className="space-y-4">
            <div className="flex gap-3 rounded-xl bg-danger-50 p-4 text-sm text-danger-800">
              <TriangleAlert aria-hidden className="size-5 shrink-0 text-danger-600" />
              <p>
                Se borrarán <strong>todos</strong> los datos de esta instalación (vehículos, cobros, cajas, usuarios y
                configuración). Exporta un respaldo antes si quieres conservarlos.
              </p>
            </div>

            <label className="flex min-h-12 cursor-pointer items-center justify-center gap-2 rounded-lg border border-dashed border-neutral-300 px-4 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-50">
              <FileUp aria-hidden className="size-5" />
              {fileName || 'Seleccionar archivo de respaldo (.json)'}
              <input
                ref={fileRef}
                type="file"
                accept="application/json,.json"
                className="sr-only"
                aria-label="Archivo de respaldo"
                onChange={handleFile}
              />
            </label>

            {fileError && <Alert>{fileError}</Alert>}

            {summary && (
              <div className="rounded-xl border border-neutral-200 p-4 text-sm">
                <p className="flex items-center gap-2 font-medium text-neutral-900">
                  <DatabaseBackup aria-hidden className="size-5 text-primary-600" />
                  Respaldo del {summary.exportedAt ? formatDateTime(summary.exportedAt) : '—'}
                </p>
                <p className="mt-1 text-neutral-600">
                  {summary.sourceDatabase ?? 'Base de datos'} · versión de esquema {summary.schemaVersion}
                  {summary.exportedBy && <> · exportado por {summary.exportedBy}</>}
                </p>
                <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1">
                  {Object.entries(BACKUP_TABLE_LABELS).map(([key, label]) => (
                    <div key={key} className="flex justify-between gap-2">
                      <dt className="text-neutral-600">{label}</dt>
                      <dd className="font-medium tabular-nums text-neutral-900">{summary.counts[key] ?? 0}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            )}

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              {summary && (
                <Button variant="secondary" onClick={resetFile}>
                  Quitar archivo
                </Button>
              )}
              <Button
                variant="danger"
                icon={<Upload aria-hidden className="size-5" />}
                disabled={!summary}
                onClick={() => {
                  setConfirmText('')
                  setImportError(null)
                  setConfirming(true)
                }}
              >
                Importar y reemplazar
              </Button>
            </div>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={confirming}
        title="¿Reemplazar todos los datos?"
        confirmLabel="Sí, reemplazar todo"
        tone="danger"
        loading={importing}
        onConfirm={handleImport}
        onCancel={() => setConfirming(false)}
      >
        <p>
          Esta acción no se puede deshacer. Al terminar se cerrará tu sesión y deberás entrar con un usuario del
          respaldo.
        </p>
        <label className="label mt-4" htmlFor="confirm-replace">
          Escribe {CONFIRM_WORD} para confirmar
        </label>
        <input
          id="confirm-replace"
          className="input font-mono uppercase"
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          autoComplete="off"
        />
        {importError && <p className="mt-2 text-danger-700">{importError}</p>}
      </ConfirmDialog>
    </div>
  )
}
