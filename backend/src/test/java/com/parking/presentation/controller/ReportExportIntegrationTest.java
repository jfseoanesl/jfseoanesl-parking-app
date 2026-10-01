package com.parking.presentation.controller;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-063: exportacion del reporte a Excel y PDF; se abren los archivos y se verifica el contenido. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportExportIntegrationTest {

    private static final String TODAY = LocalDate.now(ZoneId.of("America/Bogota")).toString();

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Nombre vigente del parqueadero: otras pruebas de la suite lo modifican (base compartida). */
    private String parkingName(MockHttpSession admin) throws Exception {
        String json = mockMvc.perform(get("/api/configuration").session(admin)).andReturn().getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.parkingName");
    }

    private MvcResult export(MockHttpSession session, String format) throws Exception {
        return mockMvc.perform(get("/api/reports/summary/export")
                        .param("from", TODAY).param("to", TODAY).param("format", format).session(session))
                .andReturn();
    }

    @Test
    void exportaExcelConLosMismosIndicadoresQueLaPantalla() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        MvcResult summary = mockMvc.perform(get("/api/reports/summary").param("from", TODAY).param("to", TODAY)
                .session(admin)).andExpect(status().isOk()).andReturn();
        String json = summary.getResponse().getContentAsString();
        long entered = ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.vehiclesEntered")).longValue();
        double collected = ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.totalCollected")).doubleValue();

        mockMvc.perform(get("/api/reports/summary/export").param("from", TODAY).param("to", TODAY)
                        .param("format", "xlsx").session(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("reporte-parqueadero_" + TODAY + ".xlsx")));

        byte[] bytes = export(admin, "xlsx").getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Reporte");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo(parkingName(admin));
            Map<String, org.apache.poi.ss.usermodel.Cell> values = new HashMap<>();
            for (Row row : sheet) {
                if (row.getCell(0) != null && row.getCell(1) != null) values.put(row.getCell(0).toString(), row.getCell(1));
            }
            // Numeros reales (no texto), iguales a los de la pantalla.
            assertThat(values.get("Vehículos ingresados").getNumericCellValue()).isEqualTo(entered);
            assertThat(values.get("Total recaudado").getNumericCellValue()).isEqualTo(collected);
        }
    }

    @Test
    void exportaPdfLegibleConEncabezadoEIndicadores() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String parkingName = parkingName(admin);
        MvcResult result = export(admin, "pdf");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5)).isEqualTo("%PDF-");

        PdfReader reader = new PdfReader(bytes);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertThat(text).contains(parkingName, "Período", "Indicadores",
                "Total recaudado", "Vehículos ingresados", "Generado por Administrador de Prueba");
    }

    @Test
    void soloElAdministradorPuedeExportar() throws Exception {
        assertThat(export(loginAs("empleado", "empleado123"), "pdf").getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void formatoOFechasInvalidasResponden400() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        mockMvc.perform(get("/api/reports/summary/export").param("from", TODAY).param("to", TODAY)
                        .param("format", "docx").session(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Formato no soportado: use xlsx, pdf o csv"));
        mockMvc.perform(get("/api/reports/summary/export").param("from", "2026-12-31").param("to", "2026-01-01")
                        .param("format", "pdf").session(admin))
                .andExpect(status().isBadRequest());
    }
}
