package com.parking.application.report;

import com.parking.application.cash.CashTransactionService;
import com.parking.application.cash.CashTransactionService.CashTransactionView;
import com.parking.domain.model.CashTransactionStatus;
import com.parking.domain.model.CashTransactionType;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RF-083: reporte de otros ingresos y gastos de caja por período, tipo, concepto y estado. */
@Service
public class CashTransactionReportService {

    /** type, conceptId y status opcionales (null = todos). */
    public record Filter(LocalDate from, LocalDate to, CashTransactionType type, String conceptId,
                         CashTransactionStatus status) {
    }

    /** Totales activos por concepto (los anulados no suman, BR-035). */
    public record ConceptRow(String type, String conceptName, long count, BigDecimal total) {
    }

    public record Report(LocalDate from, LocalDate to, BigDecimal incomeTotal, long incomeCount,
                         BigDecimal expensesTotal, long expensesCount, BigDecimal net, long voidedCount,
                         List<ConceptRow> byConcept, List<CashTransactionView> transactions) {
    }

    private final CashTransactionService transactionService;
    private final TimeProvider timeProvider;

    public CashTransactionReportService(CashTransactionService transactionService, TimeProvider timeProvider) {
        this.transactionService = transactionService;
        this.timeProvider = timeProvider;
    }

    public Report generate(Filter filter) {
        if (filter.to().isBefore(filter.from())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        ZoneId zone = timeProvider.zone();
        Instant from = filter.from().atStartOfDay(zone).toInstant();
        Instant to = filter.to().plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        List<CashTransactionView> transactions = transactionService.between(from, to).stream()
                .filter(t -> filter.type() == null || t.type().equals(filter.type().name()))
                .filter(t -> filter.conceptId() == null || filter.conceptId().isBlank()
                        || t.conceptId().equals(filter.conceptId()))
                .filter(t -> filter.status() == null || t.status().equals(filter.status().name()))
                .toList();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        long incomeCount = 0;
        long expensesCount = 0;
        long voided = 0;
        Map<String, ConceptRow> byConcept = new LinkedHashMap<>();
        for (CashTransactionView t : transactions) {
            if (!CashTransactionStatus.ACTIVE.name().equals(t.status())) {
                voided++;
                continue;
            }
            boolean isIncome = CashTransactionType.INCOME.name().equals(t.type());
            if (isIncome) {
                income = income.add(t.amount());
                incomeCount++;
            } else {
                expenses = expenses.add(t.amount());
                expensesCount++;
            }
            byConcept.merge(t.type() + "|" + t.conceptId(), new ConceptRow(t.type(), t.conceptName(), 1, t.amount()),
                    (a, b) -> new ConceptRow(a.type(), a.conceptName(), a.count() + 1, a.total().add(b.total())));
        }
        List<ConceptRow> conceptRows = new ArrayList<>(byConcept.values());
        // Primero los ingresos, luego los gastos; dentro de cada tipo, de mayor a menor valor.
        conceptRows.sort(Comparator.comparing(ConceptRow::type, Comparator.reverseOrder())
                .thenComparing(ConceptRow::total, Comparator.reverseOrder()));

        return new Report(filter.from(), filter.to(), income, incomeCount, expenses, expensesCount,
                income.subtract(expenses), voided, conceptRows, transactions);
    }
}
