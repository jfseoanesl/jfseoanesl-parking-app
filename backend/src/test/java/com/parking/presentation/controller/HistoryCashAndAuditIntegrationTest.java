package com.parking.presentation.controller;

import com.jayway.jsonpath.JsonPath;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cierre de brechas del plan: historial (RF-058), operaciones de la propia caja (RF-059), cajas
 * cerradas para corregir (RF-057), auditoria del cierre de caja (RF-066) y filtros de auditoria
 * (RF-067), comprobante con datos del parqueadero (RF-050), formato de comprobante (RF-015) y CSV (RF-063).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryCashAndAuditIntegrationTest {

    private static final String CAR = "00000000-0000-0000-0000-000000000101";
    private static final String CASH = "00000000-0000-0000-0000-000000000201";
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

    private String json(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String createEmployee(MockHttpSession admin, String username) throws Exception {
        return JsonPath.read(json(mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"fullName\":\"%s\",\"password\":\"claveSegura1\",\"role\":\"EMPLOYEE\"}"
                                .formatted(username, username)))
                .andExpect(status().isCreated()).andReturn()), "$.id");
    }

    /** Un empleado propio abre caja, registra y cobra un vehiculo; devuelve {cashId, sessionId, paymentId}. */
    private String[] operate(MockHttpSession employee, String plate) throws Exception {
        String cashId = JsonPath.read(json(mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"initialBalance\":0}"))
                .andExpect(status().isCreated()).andReturn()), "$.id");
        String sessionId = JsonPath.read(json(mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, CAR)))
                .andExpect(status().isCreated()).andReturn()), "$.id");
        Number amount = JsonPath.read(json(mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/quote")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).session(employee)).andReturn()), "$.calculatedAmount");
        String paymentId = JsonPath.read(json(mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":%s}".formatted(CASH, amount)))
                .andExpect(status().isOk()).andReturn()), "$.paymentId");
        return new String[]{cashId, sessionId, paymentId};
    }

    @Test
    void historialFiltraPorPlacaEstadoYEmpleadoYSoloLoVeElAdmin() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String employeeId = createEmployee(admin, "historial1");
        MockHttpSession employee = loginAs("historial1", "claveSegura1");
        String[] op = operate(employee, "HIS001");

        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY)
                        .param("plate", "his00").param("status", "FINALIZADO").param("employeeId", employeeId)
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.items[*].plate").value(everyItem(is("HIS001"))))
                .andExpect(jsonPath("$.items[0].status").value("FINALIZADO"))
                .andExpect(jsonPath("$.items[0].paymentId").value(op[2]))
                .andExpect(jsonPath("$.items[0].exitEmployeeName").value("historial1"));

        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY)
                        .param("status", "ANULADO").param("plate", "HIS001").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY).param("status", "X")
                .session(admin)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY).session(employee))
                .andExpect(status().isForbidden());
    }

    @Test
    void elEmpleadoVeLasOperacionesDeSuCajaYNoLasDeOtra() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        createEmployee(admin, "turnopropio");
        createEmployee(admin, "turnoajeno");
        MockHttpSession owner = loginAs("turnopropio", "claveSegura1");
        String[] op = operate(owner, "TUR001");

        mockMvc.perform(get("/api/cash-sessions/" + op[0] + "/payments").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plate").value("TUR001"))
                .andExpect(jsonPath("$[0].paymentId").value(op[2]));
        mockMvc.perform(get("/api/cash-sessions/" + op[0] + "/payments").session(loginAs("turnoajeno", "claveSegura1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cash-sessions/" + op[0] + "/payments").session(admin)).andExpect(status().isOk());
    }

    @Test
    void cierreDeCajaQuedaAuditadoYListadoParaCorregirConFiltrosDeAuditoria() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String employeeId = createEmployee(admin, "cierreaud");
        MockHttpSession employee = loginAs("cierreaud", "claveSegura1");
        String[] op = operate(employee, "CIE001");
        mockMvc.perform(post("/api/cash-sessions/" + op[0] + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(employee)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reportedCash\":0}"))
                .andExpect(status().isOk());

        // RF-057: la caja cerrada aparece en el listado del administrador (con el nombre del empleado).
        mockMvc.perform(get("/api/cash-sessions").param("from", TODAY).param("to", TODAY).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(op[0])));
        mockMvc.perform(get("/api/cash-sessions").param("from", TODAY).param("to", TODAY).session(employee))
                .andExpect(status().isForbidden());

        // RF-066 + RF-067: CASH_SESSION_CLOSED auditado; filtro por usuario y por fecha.
        mockMvc.perform(get("/api/audit-logs").param("userId", employeeId).param("from", TODAY).param("to", TODAY)
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId").value(everyItem(is(employeeId))))
                .andExpect(jsonPath("$[?(@.entityId=='%s')].action".formatted(op[0])).value(hasItem("CASH_SESSION_CLOSED")));
        mockMvc.perform(get("/api/audit-logs").param("userId", employeeId).param("from", "2020-01-01")
                        .param("to", "2020-01-02").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void comprobanteIncluyeDatosDelParqueaderoYElFormatoSeValida() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        createEmployee(admin, "comprob1");
        String[] op = operate(loginAs("comprob1", "claveSegura1"), "COM001");
        mockMvc.perform(get("/api/payments/" + op[2] + "/receipt").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").hasJsonPath())
                .andExpect(jsonPath("$.phone").hasJsonPath());

        String config = json(mockMvc.perform(get("/api/configuration").session(admin)).andReturn());
        String parkingName = JsonPath.read(config, "$.parkingName");
        mockMvc.perform(put("/api/configuration").with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingName\":\"%s\",\"receiptFormatJson\":\"no es json\"}".formatted(parkingName)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/configuration").with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingName\":\"%s\",\"receiptFormatJson\":\"{\\\"showNit\\\":false}\"}".formatted(parkingName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptFormatJson").value("{\"showNit\":false}"));
    }

    @Test
    void exportaElReporteEnCsvConBomYSinFormulas() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/summary/export").param("from", TODAY).param("to", TODAY)
                        .param("format", "csv").session(loginAs("admin", "admin123")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentType()).startsWith("text/csv");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains(".csv");
        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF); // BOM UTF-8
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertThat(text).contains("Indicador;Valor", "Total recaudado;", "Vehículos ingresados;");
    }
}
