package com.parking.infrastructure.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** OWASP A03 (CSV injection) y escapado de campos del exportador CSV (RF-063). */
class CsvReportExporterTest {

    @Test
    void neutralizaTextosQueParecenFormulas() {
        assertThat(CsvReportExporter.escape("=HYPERLINK(\"http://evil\")")).startsWith("\"'=");
        assertThat(CsvReportExporter.escape("+cmd")).isEqualTo("'+cmd");
        assertThat(CsvReportExporter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvReportExporter.escape("-2+3")).isEqualTo("'-2+3");
    }

    @Test
    void noAlteraNumerosNegativosNiTextoNormal() {
        assertThat(CsvReportExporter.escape("-1000")).isEqualTo("-1000");
        assertThat(CsvReportExporter.escape("-1000,5")).isEqualTo("-1000,5");
        assertThat(CsvReportExporter.escape("Automóvil")).isEqualTo("Automóvil");
    }

    @Test
    void entrecomillaSeparadoresYComillas() {
        assertThat(CsvReportExporter.escape("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvReportExporter.escape("dice \"hola\"")).isEqualTo("\"dice \"\"hola\"\"\"");
    }
}
