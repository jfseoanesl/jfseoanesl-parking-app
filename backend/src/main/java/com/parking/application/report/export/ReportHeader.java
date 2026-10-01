package com.parking.application.report.export;

import java.time.ZonedDateTime;

/** Datos de encabezado comunes a todos los formatos exportados. */
public record ReportHeader(
        String parkingName,
        String nit,
        String currency,
        String generatedBy,
        ZonedDateTime generatedAt
) {
}
