package com.parking.application.report.export;

import com.parking.application.report.ReportData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Contenido del reporte independiente del formato: mismas etiquetas, mismo orden y mismos valores
 * que la pantalla de Reportes. Los exportadores solo deciden como dibujarlo.
 */
public record ReportContent(List<Indicator> indicators, List<Breakdown> breakdowns) {

    public enum Kind { MONEY, COUNT, TEXT }

    /** Un indicador; {@code value} es BigDecimal para MONEY/COUNT y String para TEXT. */
    public record Indicator(String label, Kind kind, Object value) {
    }

    public record Row(String name, BigDecimal value, BigDecimal sharePercent) {
    }

    public record Breakdown(String title, Kind kind, List<Row> rows) {
    }

    public static ReportContent from(ReportData d) {
        List<Indicator> indicators = List.of(
                new Indicator("Total recaudado", Kind.MONEY, d.totalCollected()),
                new Indicator("Otros ingresos", Kind.MONEY, d.otherIncomeCollected()),
                new Indicator("Gastos de caja", Kind.MONEY, d.expensesTotal()),
                new Indicator("Neto (recaudo - gastos)", Kind.MONEY, d.netCollected()),
                new Indicator("Vehículos ingresados", Kind.COUNT, BigDecimal.valueOf(d.vehiclesEntered())),
                new Indicator("Vehículos finalizados", Kind.COUNT, BigDecimal.valueOf(d.vehiclesFinalized())),
                new Indicator("Parqueos anulados", Kind.COUNT, BigDecimal.valueOf(d.cancelledSessions())),
                new Indicator("Duración promedio", Kind.TEXT, formatDuration(d.averageDurationMinutes())),
                new Indicator("Hora de mayor movimiento", Kind.TEXT, d.peakHour() == null ? "—" : d.peakHour() + ":00"),
                new Indicator("Diferencias de caja", Kind.MONEY, d.cashDifferencesSum()),
                new Indicator("Mensualidades cobradas", Kind.MONEY, d.subscriptionCollected()),
                new Indicator("Pagos de mensualidad", Kind.COUNT, BigDecimal.valueOf(d.subscriptionPayments())),
                new Indicator("Salidas de clientes mensuales", Kind.COUNT, BigDecimal.valueOf(d.subscriberExits())));

        List<Breakdown> breakdowns = List.of(
                breakdown("Vehículos por tipo", Kind.COUNT, toDecimals(d.enteredByVehicleType())),
                breakdown("Recaudo por medio de pago", Kind.MONEY, d.collectedByPaymentMethod()),
                breakdown("Recaudo por tarifa", Kind.MONEY, d.collectedByTariff()),
                breakdown("Recaudo por empleado", Kind.MONEY, d.collectedByEmployee()));
        return new ReportContent(indicators, breakdowns);
    }

    /** Igual que formatElapsed del frontend: "45m", "1h 12m". */
    static String formatDuration(Double minutes) {
        if (minutes == null) return "—";
        long total = Math.round(minutes);
        long hours = total / 60;
        long mins = total % 60;
        return hours == 0 ? mins + "m" : hours + "h " + mins + "m";
    }

    private static Map<String, BigDecimal> toDecimals(Map<String, Long> counts) {
        java.util.LinkedHashMap<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        if (counts != null) counts.forEach((k, v) -> result.put(k, BigDecimal.valueOf(v)));
        return result;
    }

    /** Ordenado de mayor a menor, con la participacion porcentual (igual que la pantalla). */
    private static Breakdown breakdown(String title, Kind kind, Map<String, BigDecimal> values) {
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(values == null ? List.of() : values.entrySet());
        entries.sort(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()));
        BigDecimal total = entries.stream().map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Row> rows = entries.stream()
                .map(e -> new Row(e.getKey(), e.getValue(), total.signum() == 0 ? BigDecimal.ZERO
                        : e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP)))
                .toList();
        return new Breakdown(title, kind, rows);
    }
}
