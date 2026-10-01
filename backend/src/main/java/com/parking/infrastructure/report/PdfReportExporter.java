package com.parking.infrastructure.report;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.parking.application.report.ReportData;
import com.parking.application.report.export.ReportContent;
import com.parking.application.report.export.ReportContent.Breakdown;
import com.parking.application.report.export.ReportContent.Indicator;
import com.parking.application.report.export.ReportContent.Kind;
import com.parking.application.report.export.ReportContent.Row;
import com.parking.application.report.export.ReportExporter;
import com.parking.application.report.export.ReportFormat;
import com.parking.application.report.export.ReportHeader;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** RF-063: reporte en PDF (A4), con los mismos datos y orden que la pantalla de Reportes. */
@Component
public class PdfReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Paleta de la app (09-ux-specification.md §3.4): primario azul, neutros slate.
    private static final Color PRIMARY = new Color(0x25, 0x63, 0xEB);
    private static final Color TEXT = new Color(0x0F, 0x17, 0x2A);
    private static final Color MUTED = new Color(0x47, 0x55, 0x69);
    private static final Color HEADER_FILL = new Color(0xF1, 0xF5, 0xF9);
    private static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);

    // Fuentes estandar (Cp1252): cubren tildes, ñ y "—" sin incrustar archivos de fuente.
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT);
    private static final Font SMALL_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);

    @Override
    public ReportFormat format() {
        return ReportFormat.PDF;
    }

    @Override
    public byte[] export(ReportData data, ReportHeader header) {
        ReportContent content = ReportContent.from(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42, 42, 42, 42);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle("Reporte " + header.parkingName());
            document.addCreator("Parqueadero");
            document.open();

            document.add(new Paragraph(header.parkingName(), TITLE));
            if (header.nit() != null && !header.nit().isBlank()) {
                document.add(new Paragraph("NIT " + header.nit(), SMALL_MUTED));
            }
            Paragraph period = new Paragraph();
            period.add(new Chunk("Período: ", BODY_BOLD));
            period.add(new Chunk(period(data), BODY));
            period.setSpacingBefore(8);
            document.add(period);
            document.add(new Paragraph("Generado por " + header.generatedBy() + " el "
                    + DATE_TIME.format(header.generatedAt()), SMALL_MUTED));

            section(document, "Indicadores");
            PdfPTable indicators = table(new float[]{3, 2});
            headerCell(indicators, "Indicador", Element.ALIGN_LEFT);
            headerCell(indicators, "Valor", Element.ALIGN_RIGHT);
            for (Indicator indicator : content.indicators()) {
                bodyCell(indicators, indicator.label(), Element.ALIGN_LEFT, BODY);
                bodyCell(indicators, formatIndicator(indicator), Element.ALIGN_RIGHT, BODY_BOLD);
            }
            document.add(indicators);

            for (Breakdown breakdown : content.breakdowns()) {
                if (breakdown.rows().isEmpty()) continue;
                section(document, breakdown.title());
                PdfPTable table = table(new float[]{4, 2, 1.2f});
                headerCell(table, "Concepto", Element.ALIGN_LEFT);
                headerCell(table, breakdown.kind() == Kind.MONEY ? "Recaudo" : "Cantidad", Element.ALIGN_RIGHT);
                headerCell(table, "% del total", Element.ALIGN_RIGHT);
                for (Row row : breakdown.rows()) {
                    bodyCell(table, row.name(), Element.ALIGN_LEFT, BODY);
                    bodyCell(table, breakdown.kind() == Kind.MONEY ? money(row.value()) : count(row.value()),
                            Element.ALIGN_RIGHT, BODY);
                    bodyCell(table, row.sharePercent().toPlainString() + "%", Element.ALIGN_RIGHT, BODY);
                }
                document.add(table);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte", e);
        } finally {
            if (document.isOpen()) document.close();
        }
        return out.toByteArray();
    }

    private static String period(ReportData data) {
        return data.from().equals(data.to()) ? DATE.format(data.from())
                : DATE.format(data.from()) + " al " + DATE.format(data.to());
    }

    private static String formatIndicator(Indicator indicator) {
        return switch (indicator.kind()) {
            case MONEY -> money((BigDecimal) indicator.value());
            case COUNT -> count((BigDecimal) indicator.value());
            case TEXT -> (String) indicator.value();
        };
    }

    /** Formato colombiano: "$ 8.000", "-$ 1.000" (igual que la pantalla, sin decimales). */
    static String money(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        String formatted = decimalFormat().format(v.abs());
        return (v.signum() < 0 ? "-$ " : "$ ") + formatted;
    }

    private static String count(BigDecimal value) {
        return decimalFormat().format(value == null ? BigDecimal.ZERO : value);
    }

    private static DecimalFormat decimalFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols);
    }

    private static void section(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION);
        p.setSpacingBefore(16);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        return table;
    }

    private static void headerCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_BOLD));
        cell.setBackgroundColor(HEADER_FILL);
        style(cell, align);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        style(cell, align);
        table.addCell(cell);
    }

    private static void style(PdfPCell cell, int align) {
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
    }
}
