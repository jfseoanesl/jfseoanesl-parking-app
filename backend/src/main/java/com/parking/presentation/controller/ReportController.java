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

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
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
}
