package com.parking.application.report.export;

import com.parking.application.report.ReportData;
import com.parking.application.report.ReportService;
import com.parking.domain.model.Configuration;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** RF-063: exporta el mismo reporte de pantalla (ReportService) al formato pedido. */
@Service
public class ReportExportService {

    /** Archivo listo para descargar. */
    public record ExportedReport(String filename, String contentType, byte[] content) {
    }

    private final ReportService reportService;
    private final ConfigurationRepository configurationRepository;
    private final TimeProvider timeProvider;
    private final com.parking.application.report.SubscriptionReportService subscriptionReportService;
    private final Map<ReportFormat, ReportExporter> exporters = new EnumMap<>(ReportFormat.class);

    public ReportExportService(ReportService reportService, ConfigurationRepository configurationRepository,
                               TimeProvider timeProvider, List<ReportExporter> exporters,
                               com.parking.application.report.SubscriptionReportService subscriptionReportService) {
        this.subscriptionReportService = subscriptionReportService;
        this.reportService = reportService;
        this.configurationRepository = configurationRepository;
        this.timeProvider = timeProvider;
        exporters.forEach(e -> this.exporters.put(e.format(), e));
    }

    public ExportedReport export(LocalDate from, LocalDate to, ReportFormat format, String generatedBy) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new IllegalArgumentException("Formato de exportación no disponible: " + format.extension());
        }
        ReportData data = reportService.generate(from, to);

        ReportHeader header = header(generatedBy);
        String period = from.equals(to) ? from.toString() : from + "_" + to;
        String filename = "reporte-parqueadero_" + period + "." + format.extension();
        return new ExportedReport(filename, format.contentType(), exporter.export(data, header));
    }

    /** RF-078: reporte de mensualidades con los mismos filtros que la pantalla. */
    public ExportedReport exportSubscriptions(com.parking.application.report.SubscriptionReportService.Filter filter,
                                              ReportFormat format, String generatedBy) {
        ReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new IllegalArgumentException("Formato de exportación no disponible: " + format.extension());
        }
        var report = subscriptionReportService.generate(filter);
        var content = SubscriptionReportContent.from(report, filter, timeProvider.zone());
        String period = filter.from().equals(filter.to()) ? filter.from().toString() : filter.from() + "_" + filter.to();
        return new ExportedReport("reporte-mensualidades_" + period + "." + format.extension(), format.contentType(),
                exporter.exportTables(content, header(generatedBy)));
    }

    private ReportHeader header(String generatedBy) {
        Configuration config = configurationRepository.find().orElse(null);
        return new ReportHeader(
                config != null ? config.parkingName() : "Parqueadero",
                config != null ? config.nit() : null,
                config != null ? config.currency() : "COP",
                generatedBy,
                timeProvider.now().atZone(timeProvider.zone()));
    }
}
