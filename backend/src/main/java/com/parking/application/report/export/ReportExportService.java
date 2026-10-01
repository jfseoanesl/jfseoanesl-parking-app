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
    private final Map<ReportFormat, ReportExporter> exporters = new EnumMap<>(ReportFormat.class);

    public ReportExportService(ReportService reportService, ConfigurationRepository configurationRepository,
                               TimeProvider timeProvider, List<ReportExporter> exporters) {
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

        Configuration config = configurationRepository.find().orElse(null);
        ReportHeader header = new ReportHeader(
                config != null ? config.parkingName() : "Parqueadero",
                config != null ? config.nit() : null,
                config != null ? config.currency() : "COP",
                generatedBy,
                timeProvider.now().atZone(timeProvider.zone()));

        String period = from.equals(to) ? from.toString() : from + "_" + to;
        String filename = "reporte-parqueadero_" + period + "." + format.extension();
        return new ExportedReport(filename, format.contentType(), exporter.export(data, header));
    }
}
