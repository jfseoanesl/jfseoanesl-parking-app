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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-010, RF-060 a RF-065. Las aserciones comparan "antes" vs. "despues" (delta) en vez de
 * valores absolutos: la base SQLite en memoria se comparte con TODAS las clases de prueba
 * durante la ejecucion de "mvn test" (ver notas en ParkingOperationsIntegrationTest y
 * CashSessionCloseIntegrationTest), asi que otros tests ya dejan actividad "de hoy" registrada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportAndDashboardIntegrationTest {

    private static final String CAR_VEHICLE_TYPE_ID = "00000000-0000-0000-0000-000000000101";
    private static final String CASH_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000201";
    private static final String TRANSFER_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000202";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Empleado nuevo y exclusivo de la prueba (sin caja abierta). Todas las clases comparten la misma
     * base en memoria y el orden de ejecución cambia entre sistemas operativos (en Linux falló en CI),
     * así que ninguna prueba debe abrir ni esperar sin caja a los usuarios del seed (admin/empleado).
     */
    private MockHttpSession freshEmployee() throws Exception {
        String username = "emp" + java.util.UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(loginAs("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"fullName\":\"%s\",\"password\":\"claveSegura1\",\"role\":\"EMPLOYEE\"}"
                                .formatted(username, username)))
                .andExpect(status().isCreated());
        return loginAs(username, "claveSegura1");
    }

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode getJson(MockHttpSession session, String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static BigDecimal decimalAt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? BigDecimal.ZERO : new BigDecimal(value.asString());
    }

    private static BigDecimal mapValue(JsonNode node, String field, String key) {
        JsonNode map = node.get(field);
        if (map == null) return BigDecimal.ZERO;
        JsonNode value = map.get(key);
        return value == null || value.isNull() ? BigDecimal.ZERO : new BigDecimal(value.asString());
    }

    /**
     * Crea una tarifa "todo el dia" de valor fijo (prioridad baja: nunca es la sugerida) para que el
     * cobro tenga un monto real calculado por el motor; el checkout la elige via BR-008.1. Desde que
     * el backend exige chargedAmount == valor calculado, ya no se puede cobrar un monto arbitrario.
     */
    private String fixedAmountTariff(double amount) throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");
        MvcResult created = mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Fija prueba %s\",\"vehicleTypeId\":\"%s\",\"modality\":\"FULL_DAY\","
                                + "\"value\":%s,\"startTime\":\"00:00:00\",\"endTime\":\"00:00:00\","
                                + "\"priority\":99,\"validFrom\":\"2026-01-01\"}")
                                .formatted(amount, CAR_VEHICLE_TYPE_ID, amount)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private String registerAndCheckout(MockHttpSession session, String plate, String paymentMethodId,
                                        int amount) throws Exception {
        MvcResult entry = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}")
                                .formatted(plate, CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = JsonPath.read(entry.getResponse().getContentAsString(), "$.id");

        String tariffId = fixedAmountTariff(amount);
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"paymentMethodId\":\"%s\",\"chargedAmount\":%d,\"tariffId\":\"%s\"}")
                                .formatted(paymentMethodId, amount, tariffId)))
                .andExpect(status().isOk());
        return sessionId;
    }

    @Test
    void reporteDiarioYDashboardReflejanLaActividadRegistrada() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));

        JsonNode reportBefore = getJson(adminSession, "/api/reports/daily?date=" + today);
        JsonNode dashboardBefore = getJson(adminSession, "/api/dashboard");

        // Caja de un empleado propio de este escenario (no depende de las demás pruebas).
        MockHttpSession operator = freshEmployee();
        MvcResult openResult = mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":20000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cashSessionId = JsonPath.read(openResult.getResponse().getContentAsString(), "$.id");

        registerAndCheckout(operator, "REP001", CASH_PAYMENT_METHOD_ID, 5000);
        registerAndCheckout(operator, "REP002", TRANSFER_PAYMENT_METHOD_ID, 3000);

        MvcResult entry3 = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"plate\":\"REP003\",\"vehicleTypeId\":\"%s\"}").formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String session3Id = JsonPath.read(entry3.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/parking-sessions/" + session3Id + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"prueba de reporte\"}"))
                .andExpect(status().isOk());

        // Efectivo esperado = 20000 (saldo inicial) + 5000 (unico cobro en efectivo; la
        // transferencia no cuenta como fisico, BR-023.1) = 25000. Se reporta 25500 para una
        // diferencia de +500 y asi verificar cashDifferencesSum.
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":25500}"))
                .andExpect(status().isOk());

        JsonNode reportAfter = getJson(adminSession, "/api/reports/daily?date=" + today);
        JsonNode dashboardAfter = getJson(adminSession, "/api/dashboard");

        assertEquals(3, reportAfter.get("vehiclesEntered").asInt() - reportBefore.get("vehiclesEntered").asInt());
        assertEquals(2, reportAfter.get("vehiclesFinalized").asInt() - reportBefore.get("vehiclesFinalized").asInt());
        assertEquals(1, reportAfter.get("cancelledSessions").asInt() - reportBefore.get("cancelledSessions").asInt());
        assertEquals(0, BigDecimal.valueOf(8000)
                .compareTo(decimalAt(reportAfter, "totalCollected").subtract(decimalAt(reportBefore, "totalCollected"))));
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(
                mapValue(reportAfter, "collectedByPaymentMethod", "Efectivo")
                        .subtract(mapValue(reportBefore, "collectedByPaymentMethod", "Efectivo"))));
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(
                mapValue(reportAfter, "collectedByPaymentMethod", "Transferencia")
                        .subtract(mapValue(reportBefore, "collectedByPaymentMethod", "Transferencia"))));
        assertEquals(0, BigDecimal.valueOf(500)
                .compareTo(decimalAt(reportAfter, "cashDifferencesSum")
                        .subtract(decimalAt(reportBefore, "cashDifferencesSum"))));

        assertEquals(3, dashboardAfter.get("entriesToday").asInt() - dashboardBefore.get("entriesToday").asInt());
        assertEquals(2, dashboardAfter.get("exitsToday").asInt() - dashboardBefore.get("exitsToday").asInt());
        assertEquals(0, BigDecimal.valueOf(8000).compareTo(
                decimalAt(dashboardAfter, "collectedToday").subtract(decimalAt(dashboardBefore, "collectedToday"))));
        // occupied: +2 netos (3 ingresos - 2 salidas finalizadas; el anulado no cuenta como ocupado ni antes ni despues).
        assertEquals(0, dashboardAfter.get("occupied").asInt() - dashboardBefore.get("occupied").asInt());
        // openCashSessions vuelve al mismo valor: se abrio y se cerro una caja dentro de esta prueba.
        assertEquals(0, dashboardAfter.get("openCashSessions").asInt() - dashboardBefore.get("openCashSessions").asInt());
    }

    @Test
    void reportesYDashboardRequierenRolAdministrador() throws Exception {
        MockHttpSession employeeSession = loginAs("empleado", "empleado123");
        mockMvc.perform(get("/api/dashboard").session(employeeSession)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/daily?date=2026-01-01").session(employeeSession))
                .andExpect(status().isForbidden());
    }
}
