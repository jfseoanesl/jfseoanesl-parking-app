package com.parking.presentation.controller;

import com.parking.application.report.ReportService;
import com.parking.application.report.export.ReportExportService;
import com.parking.application.report.export.ReportExportService.ExportedReport;
import com.parking.application.report.export.ReportFormat;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.ReportResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** UC-010 (RF-060 a RF-063). Ver 08-api-contract.md §Reportes. */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final com.parking.application.report.SubscriptionReportService subscriptionReportService;
    private final com.parking.application.report.CashTransactionReportService cashTransactionReportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService,
                            com.parking.application.report.SubscriptionReportService subscriptionReportService,
                            com.parking.application.report.CashTransactionReportService cashTransactionReportService) {
        this.cashTransactionReportService = cashTransactionReportService;
        this.subscriptionReportService = subscriptionReportService;
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    /** Reporte diario: mismo cálculo que /summary con from = to = date. */
    @GetMapping("/daily")
    public ReportResponse daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ReportResponse.from(reportService.generate(date, date));
    }

    /** Semanal, mensual o rango personalizado: el mismo endpoint parametrizado (RF-060 a RF-063). */
    @GetMapping("/summary")
    public ReportResponse summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ReportResponse.from(reportService.generate(from, to));
    }

    /** RF-063: descarga del mismo reporte que /summary en Excel (xlsx), PDF (pdf) o CSV (csv). */
    @GetMapping("/summary/export")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam String format,
                                         @AuthenticationPrincipal ParkingUserPrincipal principal) {
        ReportFormat reportFormat = ReportFormat.fromExtension(format)
                .orElseThrow(() -> new IllegalArgumentException("Formato no soportado: use xlsx, pdf o csv"));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        ExportedReport report = reportExportService.export(from, to, reportFormat, principal.fullName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(report.filename(), StandardCharsets.UTF_8).build().toString())
                .body(report.content());
    }

    // --- RF-078: reporte de mensualidades -------------------------------------------------

    @GetMapping("/subscriptions")
    public com.parking.application.report.SubscriptionReportService.Report subscriptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        return subscriptionReportService.generate(filter(from, to, status, q));
    }

    @GetMapping("/subscriptions/export")
    public ResponseEntity<byte[]> exportSubscriptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam String format,
            @AuthenticationPrincipal ParkingUserPrincipal principal) {
        ReportFormat reportFormat = ReportFormat.fromExtension(format)
                .orElseThrow(() -> new IllegalArgumentException("Formato no soportado: use xlsx, pdf o csv"));
        ExportedReport report = reportExportService.exportSubscriptions(filter(from, to, status, q), reportFormat,
                principal.fullName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(report.filename(), StandardCharsets.UTF_8).build().toString())
                .body(report.content());
    }

    // --- RF-083: reporte de ingresos y gastos ------------------------------------------------

    @GetMapping("/cash-transactions")
    public com.parking.application.report.CashTransactionReportService.Report cashTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) com.parking.domain.model.CashTransactionType type,
            @RequestParam(required = false) String conceptId,
            @RequestParam(required = false) com.parking.domain.model.CashTransactionStatus status) {
        return cashTransactionReportService.generate(
                new com.parking.application.report.CashTransactionReportService.Filter(from, to, type, conceptId, status));
    }

    @GetMapping("/cash-transactions/export")
    public ResponseEntity<byte[]> exportCashTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) com.parking.domain.model.CashTransactionType type,
            @RequestParam(required = false) String conceptId,
            @RequestParam(required = false) com.parking.domain.model.CashTransactionStatus status,
            @RequestParam String format,
            @AuthenticationPrincipal ParkingUserPrincipal principal) {
        ReportFormat reportFormat = ReportFormat.fromExtension(format)
                .orElseThrow(() -> new IllegalArgumentException("Formato no soportado: use xlsx, pdf o csv"));
        ExportedReport report = reportExportService.exportCashTransactions(
                new com.parking.application.report.CashTransactionReportService.Filter(from, to, type, conceptId, status),
                reportFormat, principal.fullName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(report.filename(), StandardCharsets.UTF_8).build().toString())
                .body(report.content());
    }

    /** {@code status}: lista separada por comas (AL_DIA,VENCIDA,...); vacío = todos. */
    private static com.parking.application.report.SubscriptionReportService.Filter filter(
            LocalDate from, LocalDate to, String status, String q) {
        java.util.Set<com.parking.domain.model.SubscriptionStatus> statuses =
                java.util.EnumSet.noneOf(com.parking.domain.model.SubscriptionStatus.class);
        if (status != null && !status.isBlank()) {
            for (String s : status.split(",")) {
                if (s.isBlank()) continue;
                try {
                    statuses.add(com.parking.domain.model.SubscriptionStatus.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Estado inválido: use AL_DIA, EN_GRACIA, VENCIDA, SIN_PAGO o INACTIVA");
                }
            }
        }
        return new com.parking.application.report.SubscriptionReportService.Filter(from, to, statuses, q);
    }
}
