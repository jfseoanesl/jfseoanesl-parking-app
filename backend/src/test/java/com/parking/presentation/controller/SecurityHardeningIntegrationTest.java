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
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correcciones de la revision OWASP (2026-09-29). Cada prueba usa usuarios propios para no bloquear
 * ni desactivar a "admin"/"empleado", que usan las demas pruebas de la suite (contexto compartido).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions attemptLogin(MockHttpSession session, String username, String password) throws Exception {
        var request = post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
        return mockMvc.perform(session == null ? request : request.session(session));
    }

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = attemptLogin(null, username, password).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createEmployee(String username) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(loginAs("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"fullName\":\"%s\",\"password\":\"claveSegura1\",\"role\":\"EMPLOYEE\"}"
                                .formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    /** A07: session fixation. Una sesion creada antes del login no conserva su ID al autenticar. */
    @Test
    void elLoginCambiaElIdDeUnaSesionPrevia() throws Exception {
        MockHttpSession preLogin = new MockHttpSession();
        String before = preLogin.getId();

        MvcResult result = attemptLogin(preLogin, "admin", "admin123").andExpect(status().isOk()).andReturn();

        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(before);
    }

    /** A07: fuerza bruta. Tras 5 fallos el usuario queda bloqueado, aun con la contrasena correcta. */
    @Test
    void cincoIntentosFallidosBloqueanAlUsuario() throws Exception {
        createEmployee("fuerzabruta");
        for (int i = 1; i <= 5; i++) {
            attemptLogin(null, "fuerzabruta", "incorrecta" + i).andExpect(status().isUnauthorized());
        }
        attemptLogin(null, "fuerzabruta", "claveSegura1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_ATTEMPTS"))
                .andExpect(header().exists("Retry-After"));
    }

    /** A01/A07: desactivar a un usuario corta su sesion abierta en la siguiente peticion. */
    @Test
    void desactivarAUnUsuarioRevocaSuSesionAbierta() throws Exception {
        String userId = createEmployee("porrevocar");
        MockHttpSession userSession = loginAs("porrevocar", "claveSegura1");
        mockMvc.perform(get("/api/auth/me").session(userSession)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(loginAs("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session(userSession)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"REV001\",\"vehicleTypeId\":\"00000000-0000-0000-0000-000000000101\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** A01: un empleado no puede leer el resumen de la caja de otro; el dueno y el admin si. */
    @Test
    void elResumenDeCajaSoloLoVeSuDuenoOUnAdministrador() throws Exception {
        createEmployee("cajerodueno");
        createEmployee("cajeroajeno");
        MockHttpSession owner = loginAs("cajerodueno", "claveSegura1");
        MvcResult opened = mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":1000}"))
                .andExpect(status().isCreated())
                .andReturn();
        String cashId = JsonPath.read(opened.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/cash-sessions/" + cashId + "/summary").session(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/cash-sessions/" + cashId + "/summary").session(loginAs("admin", "admin123")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cash-sessions/" + cashId + "/summary").session(loginAs("cajeroajeno", "claveSegura1")))
                .andExpect(status().isForbidden());
    }
}
