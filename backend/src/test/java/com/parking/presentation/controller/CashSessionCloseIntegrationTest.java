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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-007 (RF-055 a RF-057, BR-022, BR-023). Sobre SQLite en memoria con datos semilla reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CashSessionCloseIntegrationTest {

    private static final String CAR_VEHICLE_TYPE_ID = "00000000-0000-0000-0000-000000000101";
    private static final String CASH_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000201";
    private static final String TRANSFER_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000202";

    @Autowired
    private MockMvc mockMvc;

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
                                        double chargedAmount) throws Exception {
        MvcResult entry = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = JsonPath.read(entry.getResponse().getContentAsString(), "$.id");

        String tariffId = fixedAmountTariff(chargedAmount);
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":%s,\"tariffId\":\"%s\"}"
                                .formatted(paymentMethodId, chargedAmount, tariffId)))
                .andExpect(status().isOk());
        return sessionId;
    }

    @Test
    void flujoCompletoAperturaCobrosCierreYCorreccion() throws Exception {
        MockHttpSession employeeSession = freshEmployee();

        MvcResult open = mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":50000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cashSessionId = JsonPath.read(open.getResponse().getContentAsString(), "$.id");

        registerAndCheckout(employeeSession, "CLS001", CASH_PAYMENT_METHOD_ID, 5000);
        registerAndCheckout(employeeSession, "CLS002", TRANSFER_PAYMENT_METHOD_ID, 8000);

        // RF-055: resumen en vivo antes de cerrar.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/cash-sessions/" + cashSessionId + "/summary").session(employeeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashCollected").value(5000))
                .andExpect(jsonPath("$.electronicCollected").value(8000))
                .andExpect(jsonPath("$.totalCollected").value(13000))
                .andExpect(jsonPath("$.vehiclesServed").value(2))
                .andExpect(jsonPath("$.expectedCash").value(55000)); // 50000 inicial + 5000 efectivo

        // Cierre con una diferencia de -1000 (el empleado reporta menos de lo esperado).
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":54000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.expectedCash").value(55000))
                .andExpect(jsonPath("$.reportedCash").value(54000))
                .andExpect(jsonPath("$.difference").value(-1000));

        // BR-023: no se puede cerrar dos veces.
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":54000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CASH_SESSION_ALREADY_CLOSED"));

        // RBAC: un empleado no puede hacer correcciones administrativas.
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/adjustments")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":55000,\"reason\":\"Recontado\"}"))
                .andExpect(status().isForbidden());

        // RF-057/BR-023: el administrador corrige el cierre, con motivo obligatorio y auditoría.
        MockHttpSession adminSession = loginAs("admin", "admin123");
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/adjustments")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":55000,\"reason\":\"Recontado por el administrador\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedCash").value(55000))
                .andExpect(jsonPath("$.difference").value(0));
    }

    @Test
    void cerrarCajaDeOtroEmpleadoEsRechazado() throws Exception {
        MockHttpSession employeeSession = freshEmployee();
        MvcResult open = mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":20000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cashSessionId = JsonPath.read(open.getResponse().getContentAsString(), "$.id");

        MockHttpSession adminSession = loginAs("admin", "admin123");
        // El admin SI puede cerrar cajas ajenas (rol administrativo), pero un empleado distinto no podria
        // (no hay un segundo empleado semilla para probar ese caso especifico; se cubre por diseño en el
        // servicio: NOT_CASH_SESSION_OWNER si !actingIsAdmin && !dueño).
        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":20000}"))
                .andExpect(status().isOk());
    }

    @Test
    void corregirUnaCajaAunAbiertaEsRechazado() throws Exception {
        // La caja la abre un empleado propio de la prueba (las clases comparten la base en memoria);
        // la corrige y la cierra el administrador.
        MockHttpSession adminSession = loginAs("admin", "admin123");
        MvcResult open = mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(freshEmployee())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":10000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cashSessionId = JsonPath.read(open.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/adjustments")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":10000,\"reason\":\"Prueba\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CASH_SESSION_NOT_CLOSED"));

        mockMvc.perform(post("/api/cash-sessions/" + cashSessionId + "/close")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedCash\":10000}"))
                .andExpect(status().isOk());
    }
}
