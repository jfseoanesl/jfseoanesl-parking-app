package com.parking.application.report.export;

import com.parking.application.report.ReportData;

/**
 * Puerto de exportacion de reportes (RF-063). Cada formato es una implementacion en
 * infrastructure/report; el contenido sale del mismo {@link ReportData} que muestra la pantalla.
 */
public interface ReportExporter {

    ReportFormat format();

    byte[] export(ReportData data, ReportHeader header);
}
