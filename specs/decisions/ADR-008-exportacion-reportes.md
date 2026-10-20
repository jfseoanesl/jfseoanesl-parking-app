# ADR-008 — Exportación de reportes a Excel y PDF

**Estado:** Aceptado (2026-09-29). Implementa RF-063, a pedido del usuario.

## Contexto

RF-063 exigía que la arquitectura de reportes soportara exportar a PDF, Excel y CSV, sin implementarlo en el MVP. El usuario pidió un botón para exportar los reportes a Excel o PDF.

## Decisión

**Generación en el servidor**, a partir del mismo `ReportData` que alimenta la pantalla de Reportes:

- `GET /api/reports/summary/export?from=YYYY-MM-DD&to=YYYY-MM-DD&format=xlsx|pdf` (solo ADMIN, como el resto de `/api/reports`). Responde el archivo con `Content-Disposition: attachment` y un nombre descriptivo: `reporte-parqueadero_<desde>_<hasta>.<ext>`, o solo `<fecha>` si el período es de un día.
- **Puerto `ReportExporter`** (capa de aplicación), con una implementación por formato en `infrastructure/report`:
  - `XlsxReportExporter`, con Apache POI 5.5.1 (Apache 2.0). Montos y conteos se escriben como **números con formato** (`"$" #,##0`, `#,##0`, `0%`), no como texto, así que se pueden sumar y filtrar en Excel.
  - `PdfReportExporter`, con OpenPDF 2.2.2 (LGPL/MPL). A4, paleta de la app, formato de moneda colombiano (`$ 8.000`) y fuentes estándar, que cubren tildes y ñ sin incrustar archivos.
- **`ReportContent`**: modelo intermedio común con las mismas etiquetas, orden y porcentajes que la pantalla. Los exportadores solo dibujan, así que la pantalla, el Excel y el PDF no pueden mostrar cifras distintas.
- Encabezado de ambos archivos: nombre y NIT del parqueadero (Configuración), período, y quién lo generó y cuándo (zona horaria del parqueadero, BR-026).
- Frontend: botones **Excel** y **PDF** en la cabecera de Reportes. Exportan exactamente el período que se está viendo (diario, semanal, mensual o rango) y se deshabilitan mientras carga el reporte o si el rango es inválido. Al terminar, un aviso confirma el nombre del archivo; si falla, muestra el error.

## Alternativas descartadas

- **Generar en el navegador** (SheetJS y jsPDF): sumaría cientos de KB al bundle de la PWA para todos los usuarios, incluidos los empleados, que no tienen acceso a reportes, y duplicaría en JavaScript el formato de cifras.
- **Solo "Imprimir → Guardar como PDF" del navegador:** no produce Excel, y el resultado depende del navegador y de sus márgenes.

## Consecuencias

- Agregar **CSV** requiere solo un valor nuevo en `ReportFormat` y un `ReportExporter` nuevo; el endpoint, el servicio y los botones no cambian.
- Dependencias nuevas en el backend (POI y OpenPDF): el JAR crece varios MB. Es aceptable para un servidor y no afecta la PWA.
- Verificado: pruebas de integración que abren el Excel con POI y extraen el texto del PDF con OpenPDF, además de probar el permiso (empleado → 403), el formato inválido y el rango invertido (400). Suite completa 71/71 en SQLite y MySQL. Descarga real desde el navegador; el PDF y el Excel coinciden cifra por cifra con la pantalla.

## Actualización (2026-10-20): reportes de detalle (RF-078)

- `ReportContent` solo modela indicadores y desgloses de 2 columnas. Para reportes con tablas de varias columnas, como el de mensualidades, se agregó `TableReportContent`: título, período, filtros, indicadores y tablas con columnas tipadas (TEXT, MONEY o COUNT).
- `ReportExporter` tiene un segundo método, `exportTables`, implementado en los mismos tres exportadores:
  - **XLSX**: hoja "Resumen" y una hoja por tabla, con montos numéricos, autofiltro y encabezado fijo.
  - **PDF**: A4 apaisado.
  - **CSV**: una sección por tabla.
- El texto de las celdas se escribe siempre como texto, sin interpretar fórmulas. CSV mantiene su protección contra inyección de fórmulas.

