package com.parking.application.report.export;

import com.parking.application.cash.CashTransactionService.CashTransactionView;
import com.parking.application.report.CashTransactionReportService.ConceptRow;
import com.parking.application.report.CashTransactionReportService.Filter;
import com.parking.application.report.CashTransactionReportService.Report;
import com.parking.application.report.export.ReportContent.Indicator;
import com.parking.application.report.export.ReportContent.Kind;
import com.parking.application.report.export.TableReportContent.Column;
import com.parking.application.report.export.TableReportContent.ColumnKind;
import com.parking.application.report.export.TableReportContent.Table;
import com.parking.domain.model.CashTransactionStatus;
import com.parking.domain.model.CashTransactionType;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** RF-083: el reporte de ingresos y gastos en el modelo tabular común a xlsx, pdf y csv. */
public final class CashTransactionReportContent {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CashTransactionReportContent() {
    }

    public static String typeLabel(String type) {
        return CashTransactionType.EXPENSE.name().equals(type) ? "Gasto" : "Ingreso";
    }

    public static TableReportContent from(Report report, Filter filter, String conceptName, ZoneId zone) {
        String period = report.from().equals(report.to()) ? DATE.format(report.from())
                : DATE.format(report.from()) + " al " + DATE.format(report.to());

        List<String> filters = new ArrayList<>();
        filters.add("Tipo: " + (filter.type() == null ? "ingresos y gastos"
                : filter.type() == CashTransactionType.INCOME ? "ingresos" : "gastos"));
        if (conceptName != null) filters.add("Concepto: " + conceptName);
        filters.add("Estado: " + (filter.status() == null ? "todos"
                : filter.status() == CashTransactionStatus.ACTIVE ? "activos" : "anulados"));
        filters.add("Los movimientos anulados no suman en los totales");

        List<Indicator> indicators = List.of(
                new Indicator("Otros ingresos", Kind.MONEY, report.incomeTotal()),
                new Indicator("Ingresos registrados", Kind.COUNT, BigDecimal.valueOf(report.incomeCount())),
                new Indicator("Gastos de caja", Kind.MONEY, report.expensesTotal()),
                new Indicator("Gastos registrados", Kind.COUNT, BigDecimal.valueOf(report.expensesCount())),
                new Indicator("Neto (ingresos - gastos)", Kind.MONEY, report.net()),
                new Indicator("Movimientos anulados", Kind.COUNT, BigDecimal.valueOf(report.voidedCount())));

        List<Column> conceptColumns = List.of(new Column("Tipo", ColumnKind.TEXT), new Column("Concepto", ColumnKind.TEXT),
                new Column("Movimientos", ColumnKind.COUNT), new Column("Total", ColumnKind.MONEY));
        List<List<Object>> conceptRows = new ArrayList<>();
        for (ConceptRow r : report.byConcept()) {
            conceptRows.add(Arrays.asList(typeLabel(r.type()), r.conceptName(), BigDecimal.valueOf(r.count()), r.total()));
        }

        List<Column> detailColumns = List.of(
                new Column("Fecha", ColumnKind.TEXT), new Column("Operación", ColumnKind.TEXT),
                new Column("Tipo", ColumnKind.TEXT), new Column("Concepto", ColumnKind.TEXT),
                new Column("Descripción", ColumnKind.TEXT), new Column("Medio de pago", ColumnKind.TEXT),
                new Column("Valor", ColumnKind.MONEY), new Column("Registró", ColumnKind.TEXT),
                new Column("Caja de", ColumnKind.TEXT), new Column("Estado", ColumnKind.TEXT));
        List<List<Object>> detailRows = new ArrayList<>();
        for (CashTransactionView t : report.transactions()) {
            boolean voided = CashTransactionStatus.VOIDED.name().equals(t.status());
            detailRows.add(Arrays.asList(DATE_TIME.format(t.createdAt().atZone(zone)), t.operationNumber(),
                    typeLabel(t.type()), t.conceptName(), t.description(), t.paymentMethodName(), t.amount(),
                    t.userName(), t.cashierName(), voided ? "Anulado: " + t.voidReason() : "Activo"));
        }

        return new TableReportContent("Reporte de ingresos y gastos", period, filters, indicators, List.of(
                new Table("Totales por concepto", "Conceptos", conceptColumns, conceptRows),
                new Table("Detalle de movimientos", "Movimientos", detailColumns, detailRows)));
    }
}
