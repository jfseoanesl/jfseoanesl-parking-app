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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 7 — Administración (UC-008, UC-009, UC-011, UC-012, UC-013). Las mutaciones se hacen
 * siempre sobre entidades creadas dentro de cada prueba (nunca sobre las cuentas semilla
 * admin/empleado), porque la base SQLite en memoria se comparte con las demas clases de
 * prueba durante toda la ejecucion de "mvn test".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    private static final String CAR_VEHICLE_TYPE_ID = "00000000-0000-0000-0000-000000000101";
    private static final String CASH_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000201";

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

    @Test
    void administracionDeUsuarios() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");

        MvcResult created = mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nuevoempleado\",\"fullName\":\"Nuevo Empleado\","
                                + "\"password\":\"claveInicial1\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        String newUserId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // BR (unicidad de usuario): no se puede repetir el username.
        mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nuevoempleado\",\"fullName\":\"Otro\","
                                + "\"password\":\"claveInicial1\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));

        // El nuevo usuario puede cambiar su propia contraseña, pero solo si da la actual (OWASP A07).
        MockHttpSession newUserSession = loginAs("nuevoempleado", "claveInicial1");
        mockMvc.perform(put("/api/users/me/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(newUserSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"claveNueva2\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/me/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(newUserSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"incorrecta\",\"newPassword\":\"claveNueva2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_CURRENT_PASSWORD"));
        mockMvc.perform(put("/api/users/me/password")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(newUserSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"claveInicial1\",\"newPassword\":\"claveNueva2\"}"))
                .andExpect(status().isOk());
        loginAs("nuevoempleado", "claveNueva2"); // verifica que el cambio tomo efecto

        // RF-006: desactivar.
        mockMvc.perform(patch("/api/users/" + newUserId + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // RBAC: un empleado no puede administrar usuarios.
        MockHttpSession employeeSession = loginAs("empleado", "empleado123");
        mockMvc.perform(get("/api/users").session(employeeSession)).andExpect(status().isForbidden());
    }

    @Test
    void administracionDeTarifas() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");

        // RF-021: FULL_DAY sin horario es rechazada.
        mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Invalida\",\"vehicleTypeId\":\"%s\",\"modality\":\"FULL_DAY\","
                                + "\"value\":5000,\"priority\":1}").formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isBadRequest());

        MvcResult created = mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Nocturna prueba\",\"vehicleTypeId\":\"%s\",\"modality\":\"FULL_PM\","
                                + "\"value\":9000,\"startTime\":\"18:00:00\",\"endTime\":\"06:00:00\","
                                + "\"priority\":3,\"validFrom\":\"2026-01-01\"}").formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modality").value("FULL_PM"))
                .andReturn();
        String tariffId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/tariffs/" + tariffId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Nocturna prueba editada\",\"vehicleTypeId\":\"%s\","
                                + "\"modality\":\"FULL_PM\",\"value\":9500,\"startTime\":\"19:00:00\","
                                + "\"endTime\":\"05:00:00\",\"priority\":3,\"validFrom\":\"2026-01-01\"}")
                                .formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(9500));

        mockMvc.perform(patch("/api/tariffs/" + tariffId + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // RF-067: las modificaciones de tarifa quedan auditadas.
        mockMvc.perform(get("/api/audit-logs").param("entity", "Tariff").param("entityId", tariffId)
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(tariffId))
                // La edicion guarda el horario anterior y el nuevo (antes solo nombre/valor/prioridad).
                .andExpect(jsonPath("$[?(@.action=='TARIFF_UPDATED')].oldValue")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("\"startTime\":\"18:00\""),
                                org.hamcrest.Matchers.containsString("\"endTime\":\"06:00\"")))))
                .andExpect(jsonPath("$[?(@.action=='TARIFF_UPDATED')].newValue")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("\"startTime\":\"19:00\""),
                                org.hamcrest.Matchers.containsString("\"endTime\":\"05:00\"")))));

        MockHttpSession employeeSession = loginAs("empleado", "empleado123");
        mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"X\",\"vehicleTypeId\":\"%s\",\"modality\":\"BY_TIME\","
                                + "\"value\":100,\"fractionMinutes\":60}").formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tarifaPorTiempoConValorDePrimeraFraccion() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");
        // Se crea inactiva: una BY_TIME activa aplica a toda sesion de autos y cambiaria los cobros de otras pruebas.
        String body = "{\"name\":\"Primera hora prueba\",\"vehicleTypeId\":\"%s\",\"modality\":\"%s\","
                + "\"value\":1000,\"firstFractionValue\":%s,\"fractionMinutes\":60,\"toleranceMinutes\":0,"
                + "\"startTime\":\"06:00:00\",\"endTime\":\"18:00:00\",\"priority\":5,\"active\":false}";

        MvcResult created = mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(CAR_VEHICLE_TYPE_ID, "BY_TIME", "4000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstFractionValue").value(4000))
                .andExpect(jsonPath("$.value").value(1000))
                .andReturn();
        String tariffId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Al editar sin el campo vuelve a ser una tarifa de valor uniforme.
        mockMvc.perform(put("/api/tariffs/" + tariffId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(CAR_VEHICLE_TYPE_ID, "BY_TIME", "null")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstFractionValue").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(CAR_VEHICLE_TYPE_ID, "BY_TIME", "-1")))
                .andExpect(status().isBadRequest());
        // Solo aplica a tarifas por tiempo.
        mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(CAR_VEHICLE_TYPE_ID, "FULL_AM", "4000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administracionDeConfiguracionYCatalogos() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");
        // Otras pruebas pueden haber cambiado la configuración antes: el valor "anterior" esperado es el
        // que hay justo antes de esta actualización, no el del seed.
        String before = mockMvc.perform(get("/api/configuration").session(adminSession))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        mockMvc.perform(put("/api/configuration")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingName\":\"Parqueadero Actualizado\",\"maxCapacity\":60,"
                                + "\"timezone\":\"America/Bogota\",\"currency\":\"COP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parkingName").value("Parqueadero Actualizado"))
                .andExpect(jsonPath("$.maxCapacity").value(60));

        // BR-025: el cambio de configuracion guarda el valor anterior y el nuevo (antes null/null),
        // y ambos son JSON valido.
        String audit = mockMvc.perform(get("/api/audit-logs").param("entity", "Configuration").session(adminSession))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String oldValue = JsonPath.read(audit, "$[0].oldValue");
        String newValue = JsonPath.read(audit, "$[0].newValue");
        org.junit.jupiter.api.Assertions.assertEquals((Object) JsonPath.read(before, "$.parkingName"),
                JsonPath.read(oldValue, "$.parkingName"));
        org.junit.jupiter.api.Assertions.assertEquals((Object) JsonPath.read(before, "$.maxCapacity"),
                JsonPath.read(oldValue, "$.maxCapacity"));
        org.junit.jupiter.api.Assertions.assertEquals("Parqueadero Actualizado", JsonPath.read(newValue, "$.parkingName"));
        org.junit.jupiter.api.Assertions.assertEquals(60, (int) JsonPath.read(newValue, "$.maxCapacity"));

        mockMvc.perform(post("/api/vehicle-types")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"truck\",\"name\":\"Camión\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TRUCK"));

        mockMvc.perform(post("/api/payment-methods")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"qr\",\"name\":\"Pago QR\",\"category\":\"ELECTRONIC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("QR"));

        MockHttpSession employeeSession = loginAs("empleado", "empleado123");
        mockMvc.perform(put("/api/configuration")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingName\":\"Hackeado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anulacionDeSesionConAuditoria() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");

        MvcResult entry = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"ANU001\",\"vehicleTypeId\":\"%s\"}".formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = JsonPath.read(entry.getResponse().getContentAsString(), "$.id");

        MockHttpSession employeeSession = loginAs("empleado", "empleado123");
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no autorizado\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Placa registrada por error\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULADO"));

        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"segundo intento\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));

        mockMvc.perform(get("/api/audit-logs").param("entity", "ParkingSession").param("entityId", sessionId)
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("PARKING_SESSION_CANCELLED"));
    }
}
