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
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * RF-063: reporte en CSV. Separador ";" y BOM UTF-8 para que Excel en espanol lo abra con columnas y
 * tildes correctas. Numeros sin separador de miles y con coma decimal (configuracion regional es-CO).
 * OWASP A03 (CSV injection): un texto que empiece por = + - @ o tabulador se antepone con ' para que
 * la hoja de calculo no lo interprete como formula.
 */
@Component
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEP = ";";

    @Override
    public ReportFormat format() {
        return ReportFormat.CSV;
    }

    @Override
    public byte[] export(ReportData data, ReportHeader header) {
        ReportContent content = ReportContent.from(data);
        StringBuilder csv = new StringBuilder();
        line(csv, header.parkingName());
        if (header.nit() != null && !header.nit().isBlank()) line(csv, "NIT " + header.nit());
        line(csv, "Período", period(data));
        line(csv, "Generado por", header.generatedBy(), DATE_TIME.format(header.generatedAt()));
        csv.append("\r\n");

        line(csv, "Indicador", "Valor");
        for (Indicator indicator : content.indicators()) {
            line(csv, indicator.label(), indicator.kind() == Kind.TEXT
                    ? (String) indicator.value() : number((BigDecimal) indicator.value()));
        }
        for (Breakdown breakdown : content.breakdowns()) {
            if (breakdown.rows().isEmpty()) continue;
            csv.append("\r\n");
            line(csv, breakdown.title());
            line(csv, "Concepto", breakdown.kind() == Kind.MONEY ? "Recaudo" : "Cantidad", "% del total");
            for (Row row : breakdown.rows()) {
                line(csv, row.name(), number(row.value()), row.sharePercent().toPlainString());
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // BOM: Excel detecta UTF-8
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** RF-078: encabezado, indicadores y una sección por tabla (línea de título + columnas). */
    @Override
    public byte[] exportTables(com.parking.application.report.export.TableReportContent content, ReportHeader header) {
        StringBuilder csv = new StringBuilder();
        line(csv, header.parkingName());
        if (header.nit() != null && !header.nit().isBlank()) line(csv, "NIT " + header.nit());
        line(csv, content.title());
        line(csv, "Período", content.periodLabel());
        for (String filter : content.filters()) line(csv, filter);
        line(csv, "Generado por", header.generatedBy(), DATE_TIME.format(header.generatedAt()));
        csv.append("\r\n");

        line(csv, "Indicador", "Valor");
        for (Indicator indicator : content.indicators()) {
            line(csv, indicator.label(), indicator.kind() == Kind.TEXT
                    ? (String) indicator.value() : number((BigDecimal) indicator.value()));
        }
        for (var table : content.tables()) {
            csv.append("\r\n");
            line(csv, table.title());
            line(csv, table.columns().stream().map(c -> c.label()).toArray(String[]::new));
            for (java.util.List<Object> cells : table.rows()) {
                line(csv, cells.stream().map(v -> v == null ? "" : v instanceof BigDecimal b ? number(b) : v.toString())
                        .toArray(String[]::new));
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String period(ReportData data) {
        return data.from().equals(data.to()) ? DATE.format(data.from())
                : DATE.format(data.from()) + " al " + DATE.format(data.to());
    }

    /** 1234.50 -> "1234,5"; sin separador de miles, para que la hoja lo tome como numero. */
    private static String number(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static void line(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) csv.append(SEP);
            csv.append(escape(cells[i]));
        }
        csv.append("\r\n");
    }

    static String escape(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !isPlainNumber(v)) {
            v = "'" + v; // neutraliza formulas (CSV injection)
        }
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Un numero negativo legitimo ("-1000", "-1000,5") no se trata como formula. */
    private static boolean isPlainNumber(String v) {
        return v.matches("-\\d+(,\\d+)?");
    }
}
