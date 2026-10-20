package com.parking.application.report.export;

import com.parking.application.report.export.ReportContent.Indicator;

import java.util.List;

/**
 * Contenido tabular independiente del formato (RF-078): indicadores más una o varias tablas con
 * columnas tipadas. Lo usan los reportes de detalle, como el de mensualidades. Celdas: String para
 * TEXT (fechas ya formateadas), BigDecimal para MONEY/COUNT; null se muestra vacío.
 */
public record TableReportContent(String title, String periodLabel, List<String> filters,
                                 List<Indicator> indicators, List<Table> tables) {

    public enum ColumnKind { TEXT, MONEY, COUNT }

    public record Column(String label, ColumnKind kind) {
    }

    /** {@code sheetName}: nombre corto de la hoja en Excel. */
    public record Table(String title, String sheetName, List<Column> columns, List<List<Object>> rows) {
    }
}
