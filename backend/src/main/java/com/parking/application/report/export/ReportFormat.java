package com.parking.application.report.export;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de exportacion de reportes (RF-063, ADR-008). Agregar CSV = un valor mas aqui y un
 * {@link ReportExporter} nuevo; nada mas cambia.
 */
public enum ReportFormat {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv; charset=UTF-8");

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** Busca por extension, sin distinguir mayusculas ("xlsx", "pdf"). */
    public static Optional<ReportFormat> fromExtension(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f -> f.extension.equals(normalized)).findFirst();
    }
}
