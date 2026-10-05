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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-072 / BR-027: comprobante de ingreso con consecutivo ING-000001 y configuración de impresión
 * al ingreso y a la salida (printOnEntry / printOnExit dentro de receiptFormatJson, RF-015).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntryReceiptIntegrationTest {

    private static final String CAR = "00000000-0000-0000-0000-000000000101";
    private static final String TODAY = LocalDate.now(ZoneId.of("America/Bogota")).toString();

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession admin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String json(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String register(MockHttpSession session, String plate, String notes) throws Exception {
        String body = notes == null
                ? "{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, CAR)
                : "{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\",\"notes\":\"%s\"}".formatted(plate, CAR, notes);
        return json(mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn());
    }

    private static long sequence(String entryNumber) {
        return Long.parseLong(entryNumber.substring("ING-".length()));
    }

    @Test
    void cadaIngresoRecibeUnConsecutivoUnicoYCreciente() throws Exception {
        MockHttpSession admin = admin();
        String first = JsonPath.read(register(admin, "ING001", null), "$.entryNumber");
        String second = JsonPath.read(register(admin, "ING002", null), "$.entryNumber");

        assertThat(first).matches("ING-\\d{6}");
        assertThat(second).matches("ING-\\d{6}");
        assertThat(sequence(second)).isGreaterThan(sequence(first));
    }

    @Test
    void elComprobanteDeIngresoIncluyeParqueaderoEmpleadoYObservaciones() throws Exception {
        MockHttpSession admin = admin();
        String created = register(admin, "ING003", "Rayón en la puerta");
        String id = JsonPath.read(created, "$.id");
        String entryNumber = JsonPath.read(created, "$.entryNumber");

        mockMvc.perform(get("/api/parking-sessions/" + id + "/entry-receipt").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryNumber").value(entryNumber))
                .andExpect(jsonPath("$.plate").value("ING003"))
                .andExpect(jsonPath("$.vehicleTypeName").isNotEmpty())
                .andExpect(jsonPath("$.parkingName").isNotEmpty())
                .andExpect(jsonPath("$.employeeName").isNotEmpty())
                .andExpect(jsonPath("$.notes").value("Rayón en la puerta"))
                .andExpect(jsonPath("$.status").value("EN_PARQUEADERO"));

        // El historial expone el mismo número para buscar y reimprimir.
        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY)
                        .param("plate", "ING003").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entryNumber").value(entryNumber));
    }

    @Test
    void comprobanteDeIngresoInexistenteO_SinSesion() throws Exception {
        mockMvc.perform(get("/api/parking-sessions/no-existe/entry-receipt").session(admin()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/parking-sessions/no-existe/entry-receipt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laConfiguracionGuardaLasOpcionesDeImpresion() throws Exception {
        MockHttpSession admin = admin();
        String current = json(mockMvc.perform(get("/api/configuration").session(admin)).andReturn());
        String parkingName = JsonPath.read(current, "$.parkingName");
        String format = "{\\\"printOnEntry\\\":true,\\\"printOnExit\\\":false}";

        mockMvc.perform(put("/api/configuration")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parkingName\":\"%s\",\"receiptFormatJson\":\"%s\"}".formatted(parkingName, format)))
                .andExpect(status().isOk());

        String saved = JsonPath.read(json(mockMvc.perform(get("/api/configuration").session(admin)).andReturn()),
                "$.receiptFormatJson");
        assertThat(saved).contains("\"printOnEntry\":true").contains("\"printOnExit\":false");
    }
}
