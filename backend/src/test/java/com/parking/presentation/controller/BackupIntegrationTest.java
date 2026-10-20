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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-073 / ADR-009: respaldo completo (exportar) y restauración que reemplaza todo (importar).
 * Cada prueba que importa restaura el mismo estado que exportó, así no afecta a las demás clases
 * que comparten la base en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackupIntegrationTest {

    private static final String CAR = "00000000-0000-0000-0000-000000000101";

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

    private String export(MockHttpSession admin) throws Exception {
        return json(mockMvc.perform(get("/api/admin/backup").session(admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"parking-respaldo_")))
                .andReturn());
    }

    private MvcResult importBackup(MockHttpSession admin, String body) throws Exception {
        return mockMvc.perform(post("/api/admin/backup/import")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
    }

    private String registerEntry(MockHttpSession session, String plate) throws Exception {
        return JsonPath.read(json(mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, CAR)))
                .andExpect(status().isCreated()).andReturn()), "$.id");
    }

    @Test
    void elRespaldoIncluyeTodasLasTablasConConteosYSinContrasenasEnTextoPlano() throws Exception {
        String backup = export(loginAs("admin", "admin123"));

        assertThat((String) JsonPath.read(backup, "$.format")).isEqualTo("parking-backup");
        assertThat((Integer) JsonPath.read(backup, "$.formatVersion")).isEqualTo(1);
        assertThat((String) JsonPath.read(backup, "$.schemaVersion")).isNotBlank();
        Map<String, Integer> counts = JsonPath.read(backup, "$.counts");
        assertThat(counts).containsKeys("roles", "users", "vehicleTypes", "paymentMethods", "configuration",
                "tariffs", "cashSessions", "parkingSessions", "payments", "cashMovements", "auditLogs",
                "operationCounters");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            List<?> rows = JsonPath.read(backup, "$.tables." + e.getKey());
            assertThat(rows).as(e.getKey()).hasSize(e.getValue());
        }
        List<String> hashes = JsonPath.read(backup, "$.tables.users[*].passwordHash");
        assertThat(hashes).isNotEmpty().allMatch(h -> h.startsWith("$2"));
        assertThat(backup).doesNotContain("admin123");
    }

    @Test
    void importarRestauraExactamenteElEstadoDelRespaldo() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String backup = export(admin);
        int sessionsInBackup = JsonPath.read(backup, "$.counts.parkingSessions");
        String entryCounter = JsonPath.<List<Object>>read(backup,
                "$.tables.operationCounters[?(@.name == 'ENTRY')].counterValue").get(0).toString();

        // Cambios posteriores al respaldo: deben desaparecer al importar.
        String afterBackup = registerEntry(admin, "BKP001");

        MvcResult result = importBackup(admin, backup);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(json(result), "$.counts.parkingSessions")).isEqualTo(sessionsInBackup);

        mockMvc.perform(get("/api/parking-sessions/" + afterBackup).session(admin))
                .andExpect(status().isNotFound());

        String restored = export(admin);
        assertThat((Integer) JsonPath.read(restored, "$.counts.parkingSessions")).isEqualTo(sessionsInBackup);
        // El consecutivo ING vuelve al del respaldo: el siguiente ingreso continúa desde ahí.
        String restoredCounter = JsonPath.<List<Object>>read(restored,
                "$.tables.operationCounters[?(@.name == 'ENTRY')].counterValue").get(0).toString();
        assertThat(restoredCounter).isEqualTo(entryCounter);
        String next = JsonPath.read(json(mockMvc.perform(get("/api/parking-sessions/" + registerEntry(admin, "BKP002"))
                .session(admin)).andReturn()), "$.entryNumber");
        assertThat(next).isEqualTo("ING-%06d".formatted(Long.parseLong(entryCounter) + 1));

        // La importación queda auditada (el admin que importó existe en los datos restaurados).
        mockMvc.perform(get("/api/audit-logs").param("entity", "Database").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action").value(hasItem("DATABASE_IMPORTED")));
    }

    @Test
    void rechazaRespaldosInvalidosSinTocarLosDatos() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String backup = export(admin);
        String plate = "BKP003";
        String sessionId = registerEntry(admin, plate);

        MvcResult wrongFormat = importBackup(admin, "{\"format\":\"otra-cosa\"}");
        assertThat(wrongFormat.getResponse().getStatus()).isEqualTo(400);

        String schema = JsonPath.read(backup, "$.schemaVersion");
        MvcResult wrongVersion = importBackup(admin,
                backup.replace("\"schemaVersion\":\"" + schema + "\"", "\"schemaVersion\":\"999\""));
        assertThat(wrongVersion.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(wrongVersion)).contains("versión 999");

        MvcResult withoutAdmin = importBackup(admin, backup.replace("\"active\":true", "\"active\":false"));
        assertThat(withoutAdmin.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(withoutAdmin)).contains("administrador activo");

        // Nada cambió: el ingreso creado después del respaldo sigue existiendo.
        mockMvc.perform(get("/api/parking-sessions/" + sessionId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value(plate));
    }

    @Test
    void soloElAdministradorPuedeExportarEImportar() throws Exception {
        mockMvc.perform(get("/api/admin/backup")).andExpect(status().isUnauthorized());

        MockHttpSession employee = loginAs("empleado", "empleado123");
        mockMvc.perform(get("/api/admin/backup").session(employee)).andExpect(status().isForbidden());
        MvcResult imported = importBackup(employee, "{}");
        assertThat(imported.getResponse().getStatus()).isEqualTo(403);
    }
}
