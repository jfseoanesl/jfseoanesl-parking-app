package com.parking.application.report.export;

import com.parking.application.report.SubscriptionReportService.Filter;
import com.parking.application.report.SubscriptionReportService.PaymentRow;
import com.parking.application.report.SubscriptionReportService.PortfolioRow;
import com.parking.application.report.SubscriptionReportService.Report;
import com.parking.application.report.export.ReportContent.Indicator;
import com.parking.application.report.export.ReportContent.Kind;
import com.parking.application.report.export.TableReportContent.Column;
import com.parking.application.report.export.TableReportContent.ColumnKind;
import com.parking.application.report.export.TableReportContent.Table;
import com.parking.domain.model.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RF-078: el reporte de mensualidades en el modelo tabular común a xlsx, pdf y csv. */
public final class SubscriptionReportContent {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Mismas etiquetas que la pantalla (frontend lib/subscription.ts). */
    public static final Map<SubscriptionStatus, String> STATUS_LABELS = Map.of(
            SubscriptionStatus.AL_DIA, "Al día",
            SubscriptionStatus.EN_GRACIA, "En gracia",
            SubscriptionStatus.VENCIDA, "Vencida",
            SubscriptionStatus.SIN_PAGO, "Sin pago",
            SubscriptionStatus.INACTIVA, "Inactiva");

    private SubscriptionReportContent() {
    }

    public static TableReportContent from(Report report, Filter filter) {
        return from(report, filter, ZoneId.of("America/Bogota"));
    }

    public static TableReportContent from(Report report, Filter filter, ZoneId zone) {
        String period = report.from().equals(report.to()) ? DATE.format(report.from())
                : DATE.format(report.from()) + " al " + DATE.format(report.to());

        List<String> filters = new ArrayList<>();
        filters.add("Estados: " + (filter.statuses() == null || filter.statuses().isEmpty() ? "todos"
                : Arrays.stream(SubscriptionStatus.values()).filter(filter.statuses()::contains)
                .map(STATUS_LABELS::get).collect(Collectors.joining(", "))));
        if (filter.query() != null && !filter.query().isBlank()) {
            filters.add("Búsqueda: " + filter.query().trim());
        }
        filters.add("Estado y días de mora calculados al " + DATE.format(report.today()));

        Map<SubscriptionStatus, Long> byStatus = report.countByStatus();
        List<Indicator> indicators = List.of(
                new Indicator("Recaudo de mensualidades", Kind.MONEY, report.collected()),
                new Indicator("Pagos registrados", Kind.COUNT, BigDecimal.valueOf(report.paymentsCount())),
                new Indicator("Mensualidades activas", Kind.COUNT, BigDecimal.valueOf(report.activeCount())),
                new Indicator("Al día", Kind.COUNT, BigDecimal.valueOf(byStatus.getOrDefault(SubscriptionStatus.AL_DIA, 0L))),
                new Indicator("En gracia", Kind.COUNT, BigDecimal.valueOf(byStatus.getOrDefault(SubscriptionStatus.EN_GRACIA, 0L))),
                new Indicator("Vencidas", Kind.COUNT, BigDecimal.valueOf(byStatus.getOrDefault(SubscriptionStatus.VENCIDA, 0L))),
                new Indicator("Sin pago", Kind.COUNT, BigDecimal.valueOf(byStatus.getOrDefault(SubscriptionStatus.SIN_PAGO, 0L))),
                new Indicator("Inactivas", Kind.COUNT, BigDecimal.valueOf(byStatus.getOrDefault(SubscriptionStatus.INACTIVA, 0L))),
                new Indicator("Cartera vencida estimada", Kind.MONEY, report.overdueAmount()));

        List<Column> portfolioColumns = List.of(
                new Column("Cliente", ColumnKind.TEXT), new Column("Documento", ColumnKind.TEXT),
                new Column("Teléfono", ColumnKind.TEXT), new Column("Placas", ColumnKind.TEXT),
                new Column("Valor mensual", ColumnKind.MONEY), new Column("Día de corte", ColumnKind.COUNT),
                new Column("Pagada hasta", ColumnKind.TEXT), new Column("Estado", ColumnKind.TEXT),
                new Column("Días de mora", ColumnKind.COUNT), new Column("Pagado en el período", ColumnKind.MONEY),
                new Column("Pagos en el período", ColumnKind.COUNT));
        List<List<Object>> portfolioRows = new ArrayList<>();
        for (PortfolioRow r : report.portfolio()) {
            portfolioRows.add(Arrays.asList(r.customerName(), r.documentId(), r.phone(), String.join(", ", r.plates()),
                    r.monthlyFee(), BigDecimal.valueOf(r.cutOffDay()), date(r.paidUntil()), STATUS_LABELS.get(r.status()),
                    r.daysOverdue() == null ? null : BigDecimal.valueOf(r.daysOverdue()), r.paidInPeriod(),
                    BigDecimal.valueOf(r.paymentsInPeriod())));
        }

        List<Column> paymentColumns = List.of(
                new Column("Fecha de pago", ColumnKind.TEXT), new Column("Operación", ColumnKind.TEXT),
                new Column("Cliente", ColumnKind.TEXT), new Column("Documento", ColumnKind.TEXT),
                new Column("Período desde", ColumnKind.TEXT), new Column("Período hasta", ColumnKind.TEXT),
                new Column("Valor", ColumnKind.MONEY), new Column("Medio de pago", ColumnKind.TEXT),
                new Column("Cobró", ColumnKind.TEXT));
        List<List<Object>> paymentRows = new ArrayList<>();
        for (PaymentRow p : report.payments()) {
            paymentRows.add(Arrays.asList(DATE_TIME.format(p.paidAt().atZone(zone)), p.operationNumber(),
                    p.customerName(), p.documentId(), date(p.periodFrom()), date(p.periodTo()), p.amount(),
                    p.paymentMethodName(), p.employeeName()));
        }

        return new TableReportContent("Reporte de mensualidades", period, filters, indicators, List.of(
                new Table("Estado de cartera", "Cartera", portfolioColumns, portfolioRows),
                new Table("Detalle de pagos", "Pagos", paymentColumns, paymentRows)));
    }

    private static String date(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }
}
