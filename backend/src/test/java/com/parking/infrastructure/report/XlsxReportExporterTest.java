package com.parking.infrastructure.report;

import com.parking.application.report.ReportData;
import com.parking.application.report.export.ReportHeader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OWASP A03 (inyeccion de formulas / CSV injection): un texto controlado por usuarios (nombre de
 * tarifa, empleado, parqueadero) que empiece por = + - @ debe quedar como TEXTO en el Excel, nunca
 * como formula que Excel ejecute al abrir el archivo.
 */
class XlsxReportExporterTest {

    @Test
    void textosQueParecenFormulasSeEscribenComoTextoNoComoFormula() throws Exception {
        String malicious = "=HYPERLINK(\"http://evil.example\",\"clic\")";
        ReportData data = new ReportData(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 29), 1, 1,
                Map.of("+Auto", 1L), BigDecimal.valueOf(1000), Map.of("@Efectivo", BigDecimal.valueOf(1000)),
                Map.of(malicious, BigDecimal.valueOf(1000)), 10.0, 8, Map.of("-Empleado", BigDecimal.valueOf(1000)),
                0, BigDecimal.ZERO);
        ReportHeader header = new ReportHeader("=CMD()", null, "COP", "=Admin",
                ZonedDateTime.of(2026, 9, 29, 12, 0, 0, 0, ZoneId.of("America/Bogota")));

        byte[] bytes = new XlsxReportExporter().export(data, header);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundMalicious = false;
            for (Row row : sheet) {
                for (Cell cell : row) {
                    assertThat(cell.getCellType()).as("celda %s", cell.getAddress()).isNotEqualTo(CellType.FORMULA);
                    if (cell.getCellType() == CellType.STRING && malicious.equals(cell.getStringCellValue())) {
                        foundMalicious = true;
                    }
                }
            }
            assertThat(foundMalicious).as("el texto aparece literal, como texto").isTrue();
        }
    }
}
