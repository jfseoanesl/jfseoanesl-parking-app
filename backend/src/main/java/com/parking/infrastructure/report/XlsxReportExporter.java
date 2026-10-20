package com.parking.infrastructure.report;

import com.parking.application.report.ReportData;
import com.parking.application.report.export.ReportContent;
import com.parking.application.report.export.ReportContent.Breakdown;
import com.parking.application.report.export.ReportContent.Indicator;
import com.parking.application.report.export.ReportContent.Kind;
import com.parking.application.report.export.ReportContent.Row;
import com.parking.application.report.export.ReportExporter;
import com.parking.application.report.export.ReportFormat;
import com.parking.application.report.export.ReportHeader;
import com.parking.application.report.export.TableReportContent;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF-063: reporte en Excel. Montos y conteos se escriben como NUMEROS con formato (no texto), para
 * que se puedan sumar, filtrar o graficar en Excel.
 */
@Component
public class XlsxReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ReportFormat format() {
        return ReportFormat.XLSX;
    }

    @Override
    public byte[] export(ReportData data, ReportHeader header) {
        ReportContent content = ReportContent.from(data);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            Sheet sheet = workbook.createSheet("Reporte");
            int r = 0;

            text(sheet, r++, 0, header.parkingName(), styles.title);
            if (header.nit() != null && !header.nit().isBlank()) {
                text(sheet, r++, 0, "NIT " + header.nit(), styles.muted);
            }
            text(sheet, r++, 0, "Período: " + period(data), styles.plain);
            text(sheet, r++, 0, "Generado por " + header.generatedBy() + " el "
                    + DATE_TIME.format(header.generatedAt()), styles.muted);
            r++;

            text(sheet, r++, 0, "Indicadores", styles.section);
            text(sheet, r, 0, "Indicador", styles.tableHeader);
            text(sheet, r++, 1, "Valor", styles.tableHeader);
            for (Indicator indicator : content.indicators()) {
                text(sheet, r, 0, indicator.label(), styles.cell);
                if (indicator.kind() == Kind.TEXT) {
                    text(sheet, r, 1, (String) indicator.value(), styles.cellRight);
                } else {
                    number(sheet, r, 1, (BigDecimal) indicator.value(),
                            indicator.kind() == Kind.MONEY ? styles.money : styles.count);
                }
                r++;
            }

            for (Breakdown breakdown : content.breakdowns()) {
                if (breakdown.rows().isEmpty()) continue;
                r++;
                text(sheet, r++, 0, breakdown.title(), styles.section);
                text(sheet, r, 0, "Concepto", styles.tableHeader);
                text(sheet, r, 1, breakdown.kind() == Kind.MONEY ? "Recaudo" : "Cantidad", styles.tableHeader);
                text(sheet, r++, 2, "% del total", styles.tableHeader);
                for (Row row : breakdown.rows()) {
                    text(sheet, r, 0, row.name(), styles.cell);
                    number(sheet, r, 1, row.value(), breakdown.kind() == Kind.MONEY ? styles.money : styles.count);
                    number(sheet, r, 2, row.sharePercent().movePointLeft(2), styles.percent);
                    r++;
                }
            }

            sheet.setColumnWidth(0, 34 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.setFitToPage(true);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte", e);
        }
    }

    /** RF-078: hoja "Resumen" (encabezado, filtros e indicadores) y una hoja por tabla, con autofiltro. */
    @Override
    public byte[] exportTables(TableReportContent content, ReportHeader header) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            Sheet summary = workbook.createSheet("Resumen");
            int r = 0;
            text(summary, r++, 0, header.parkingName(), styles.title);
            if (header.nit() != null && !header.nit().isBlank()) {
                text(summary, r++, 0, "NIT " + header.nit(), styles.muted);
            }
            text(summary, r++, 0, content.title(), styles.section);
            text(summary, r++, 0, "Período: " + content.periodLabel(), styles.plain);
            for (String filter : content.filters()) {
                text(summary, r++, 0, filter, styles.plain);
            }
            text(summary, r++, 0, "Generado por " + header.generatedBy() + " el "
                    + DATE_TIME.format(header.generatedAt()), styles.muted);
            r++;
            text(summary, r, 0, "Indicador", styles.tableHeader);
            text(summary, r++, 1, "Valor", styles.tableHeader);
            for (Indicator indicator : content.indicators()) {
                text(summary, r, 0, indicator.label(), styles.cell);
                if (indicator.kind() == Kind.TEXT) {
                    text(summary, r, 1, (String) indicator.value(), styles.cellRight);
                } else {
                    number(summary, r, 1, (BigDecimal) indicator.value(),
                            indicator.kind() == Kind.MONEY ? styles.money : styles.count);
                }
                r++;
            }
            summary.setColumnWidth(0, 40 * 256);
            summary.setColumnWidth(1, 18 * 256);

            for (TableReportContent.Table table : content.tables()) {
                Sheet sheet = workbook.createSheet(table.sheetName());
                int row = 0;
                for (int c = 0; c < table.columns().size(); c++) {
                    text(sheet, row, c, table.columns().get(c).label(), styles.tableHeader);
                }
                row++;
                for (List<Object> cells : table.rows()) {
                    for (int c = 0; c < table.columns().size(); c++) {
                        Object value = cells.get(c);
                        TableReportContent.ColumnKind kind = table.columns().get(c).kind();
                        if (kind == TableReportContent.ColumnKind.TEXT || !(value instanceof BigDecimal)) {
                            // Texto como celda de texto: nunca se interpreta como formula (CSV/Excel injection).
                            text(sheet, row, c, value == null ? "" : value.toString(), styles.cell);
                        } else {
                            number(sheet, row, c, (BigDecimal) value,
                                    kind == TableReportContent.ColumnKind.MONEY ? styles.money : styles.count);
                        }
                    }
                    row++;
                }
                if (!table.rows().isEmpty()) {
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, row - 1, 0, table.columns().size() - 1));
                }
                sheet.createFreezePane(0, 1);
                for (int c = 0; c < table.columns().size(); c++) {
                    sheet.setColumnWidth(c, (table.columns().get(c).kind() == TableReportContent.ColumnKind.TEXT ? 22 : 15) * 256);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte", e);
        }
    }

    private static String period(ReportData data) {
        return data.from().equals(data.to()) ? DATE.format(data.from())
                : DATE.format(data.from()) + " al " + DATE.format(data.to());
    }

    private static void text(Sheet sheet, int row, int col, String value, CellStyle style) {
        Cell cell = cell(sheet, row, col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void number(Sheet sheet, int row, int col, BigDecimal value, CellStyle style) {
        Cell cell = cell(sheet, row, col);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private static Cell cell(Sheet sheet, int row, int col) {
        org.apache.poi.ss.usermodel.Row r = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        return r.createCell(col);
    }

    /** Estilos del libro (POI limita la cantidad de estilos: se crean una sola vez). */
    private static final class Styles {
        final CellStyle title, section, plain, muted, tableHeader, cell, cellRight, money, count, percent;

        Styles(XSSFWorkbook wb) {
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            title = wb.createCellStyle();
            title.setFont(titleFont);

            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            section = wb.createCellStyle();
            section.setFont(boldFont);

            plain = wb.createCellStyle();

            Font mutedFont = wb.createFont();
            mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            muted = wb.createCellStyle();
            muted.setFont(mutedFont);

            tableHeader = bordered(wb);
            tableHeader.setFont(boldFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            cell = bordered(wb);
            cellRight = bordered(wb);
            cellRight.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);

            short money0 = wb.createDataFormat().getFormat("\"$\" #,##0");
            money = bordered(wb);
            money.setDataFormat(money0);

            count = bordered(wb);
            count.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            percent = bordered(wb);
            percent.setDataFormat(wb.createDataFormat().getFormat("0%"));
        }

        private static CellStyle bordered(XSSFWorkbook wb) {
            CellStyle style = wb.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return style;
        }
    }
}
