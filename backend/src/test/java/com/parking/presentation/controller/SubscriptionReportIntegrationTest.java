package com.parking.presentation.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-078: reporte de mensualidades (cartera, pagos, indicadores, filtros y exportación). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionReportIntegrationTest {

    private static final String CAR = "00000000-0000-0000-0000-000000000101";
    private static final String CASH = "00000000-0000-0000-0000-000000000201";
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Bogota"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String json(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MvcResult postJson(MockHttpSession s, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(SecurityMockMvcRequestPostProcessors.csrf()).session(s)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private String create(MockHttpSession admin, String name, String document, int fee, String plate) throws Exception {
        MvcResult r = postJson(admin, "/api/subscriptions",
                "{\"customerName\":\"%s\",\"documentId\":\"%s\",\"monthlyFee\":%d,\"cutOffDay\":5,\"vehicles\":[{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}]}"
                        .formatted(name, document, fee, plate, CAR));
        assertThat(r.getResponse().getStatus()).as(json(r)).isEqualTo(201);
        return JsonPath.read(json(r), "$.id");
    }

    private String report(MockHttpSession admin, String status, String q) throws Exception {
        var request = get("/api/reports/subscriptions").param("from", TODAY.toString()).param("to", TODAY.toString())
                .param("q", q).session(admin);
        if (status != null) request = request.param("status", status);
        MvcResult r = mockMvc.perform(request).andReturn();
        assertThat(r.getResponse().getStatus()).as(json(r)).isEqualTo(200);
        return json(r);
    }

    @Test
    void carteraPagosIndicadoresYFiltros() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        if (mockMvc.perform(get("/api/cash-sessions/current").session(admin)).andReturn().getResponse().getStatus() == 404) {
            postJson(admin, "/api/cash-sessions/open", "{\"initialBalance\":0}");
        }
        String paid = create(admin, "RPT Pagado", "RPT-1", 80000, "RPT101");
        create(admin, "RPT Sin Pago", "RPT-2", 60000, "RPT102");
        String overdue = create(admin, "RPT Vencido", "RPT-3", 70000, "RPT103");
        String inactive = create(admin, "RPT Inactivo", "RPT-4", 50000, "RPT104");

        assertThat(postJson(admin, "/api/subscriptions/" + paid + "/payments",
                "{\"paymentMethodId\":\"%s\",\"amount\":80000}".formatted(CASH)).getResponse().getStatus()).isEqualTo(201);
        // Vencida: pagó hasta hace 20 días (más que los 3 días de gracia).
        jdbcTemplate.update("UPDATE subscription SET paid_until = ? WHERE id = ?", TODAY.minusDays(20).toString(), overdue);
        mockMvc.perform(patch("/api/subscriptions/" + inactive + "/status")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());

        String all = report(admin, null, "rpt");
        assertThat((List<?>) JsonPath.read(all, "$.portfolio")).hasSize(4);
        // Primero lo que requiere gestión: la vencida con sus días de mora.
        assertThat((String) JsonPath.read(all, "$.portfolio[0].customerName")).isEqualTo("RPT Vencido");
        assertThat((Integer) JsonPath.read(all, "$.portfolio[0].daysOverdue")).isEqualTo(20);
        assertThat((String) JsonPath.read(all, "$.portfolio[1].status")).isEqualTo("SIN_PAGO");
        assertThat((Integer) JsonPath.read(all, "$.countByStatus.AL_DIA")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(all, "$.countByStatus.INACTIVA")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(all, "$.activeCount")).isEqualTo(3);
        // Cartera vencida estimada = vencida (70.000) + sin pago (60.000).
        assertThat(((Number) JsonPath.read(all, "$.overdueAmount")).intValue()).isEqualTo(130000);
        assertThat(((Number) JsonPath.read(all, "$.collected")).intValue()).isEqualTo(80000);
        assertThat((String) JsonPath.read(all, "$.payments[0].operationNumber")).matches("OP-\\d{6}");
        assertThat((String) JsonPath.read(all, "$.payments[0].customerName")).isEqualTo("RPT Pagado");

        // Estado múltiple.
        String overdueOnly = report(admin, "VENCIDA,SIN_PAGO", "rpt");
        assertThat((List<String>) JsonPath.read(overdueOnly, "$.portfolio[*].customerName"))
                .containsExactlyInAnyOrder("RPT Vencido", "RPT Sin Pago");
        assertThat((List<?>) JsonPath.read(overdueOnly, "$.payments")).isEmpty();

        // Búsqueda por placa con espacios y por documento.
        assertThat((List<String>) JsonPath.read(report(admin, null, "rpt 101"), "$.portfolio[*].customerName"))
                .containsExactly("RPT Pagado");
        assertThat((List<String>) JsonPath.read(report(admin, null, "RPT-3"), "$.portfolio[*].customerName"))
                .containsExactly("RPT Vencido");

        // Un rango que no incluye hoy no trae pagos (la cartera sí se lista, con estado a hoy).
        String yesterday = json(mockMvc.perform(get("/api/reports/subscriptions")
                .param("from", TODAY.minusDays(2).toString()).param("to", TODAY.minusDays(1).toString())
                .param("q", "rpt").session(admin)).andReturn());
        assertThat((List<?>) JsonPath.read(yesterday, "$.payments")).isEmpty();
        assertThat((List<?>) JsonPath.read(yesterday, "$.portfolio")).hasSize(4);
    }

    @Test
    void exportaExcelPdfYCsvConLosFiltros() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        create(admin, "EXP Cliente", "EXP-1", 45000, "EXP101");

        MvcResult xlsx = mockMvc.perform(get("/api/reports/subscriptions/export").param("from", TODAY.toString())
                .param("to", TODAY.toString()).param("q", "EXP").param("format", "xlsx").session(admin)).andReturn();
        assertThat(xlsx.getResponse().getStatus()).isEqualTo(200);
        assertThat(xlsx.getResponse().getHeader("Content-Disposition")).contains("reporte-mensualidades_" + TODAY + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getSheet("Resumen")).isNotNull();
            var cartera = workbook.getSheet("Cartera");
            assertThat(cartera.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Cliente");
            assertThat(cartera.getRow(1).getCell(0).getStringCellValue()).isEqualTo("EXP Cliente");
            assertThat(cartera.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(45000.0);
            assertThat(workbook.getSheet("Pagos")).isNotNull();
        }

        MvcResult pdf = mockMvc.perform(get("/api/reports/subscriptions/export").param("from", TODAY.toString())
                .param("to", TODAY.toString()).param("q", "EXP").param("format", "pdf").session(admin)).andReturn();
        String text = new PdfTextExtractor(new PdfReader(pdf.getResponse().getContentAsByteArray())).getTextFromPage(1);
        assertThat(text).contains("Reporte de mensualidades").contains("EXP Cliente");

        MvcResult csv = mockMvc.perform(get("/api/reports/subscriptions/export").param("from", TODAY.toString())
                .param("to", TODAY.toString()).param("q", "EXP").param("format", "csv").session(admin)).andReturn();
        byte[] bytes = csv.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertThat(content).contains("Estado de cartera").contains("Detalle de pagos").contains("EXP Cliente;EXP-1");
    }

    @Test
    void validacionesYPermisos() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        assertThat(mockMvc.perform(get("/api/reports/subscriptions").param("from", TODAY.toString())
                .param("to", TODAY.toString()).param("status", "OTRO").session(admin)).andReturn()
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(mockMvc.perform(get("/api/reports/subscriptions").param("from", TODAY.toString())
                .param("to", TODAY.minusDays(1).toString()).session(admin)).andReturn()
                .getResponse().getStatus()).isEqualTo(400);
        MockHttpSession employee = loginAs("empleado", "empleado123");
        assertThat(mockMvc.perform(get("/api/reports/subscriptions").param("from", TODAY.toString())
                .param("to", TODAY.toString()).session(employee)).andReturn().getResponse().getStatus()).isEqualTo(403);
    }
}
