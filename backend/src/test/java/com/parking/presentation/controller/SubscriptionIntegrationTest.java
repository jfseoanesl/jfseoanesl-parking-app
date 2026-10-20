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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-074 a RF-077 / BR-028 a BR-031: mensualidades de punta a punta. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionIntegrationTest {

    private static final String CAR = "00000000-0000-0000-0000-000000000101";
    private static final String MOTO = "00000000-0000-0000-0000-000000000102";
    private static final String CASH = "00000000-0000-0000-0000-000000000201";
    private static final String TODAY = LocalDate.now(ZoneId.of("America/Bogota")).toString();

    @Autowired
    private MockMvc mockMvc;

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

    private MvcResult postJson(MockHttpSession session, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(SecurityMockMvcRequestPostProcessors.csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private String createSubscription(MockHttpSession admin, String name, int fee, String... plates) throws Exception {
        StringBuilder vehicles = new StringBuilder();
        for (int i = 0; i < plates.length; i++) {
            if (i > 0) vehicles.append(',');
            vehicles.append("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plates[i], i == 0 ? CAR : MOTO));
        }
        MvcResult r = postJson(admin, "/api/subscriptions",
                "{\"customerName\":\"%s\",\"documentId\":\"123\",\"monthlyFee\":%d,\"cutOffDay\":5,\"vehicles\":[%s]}"
                        .formatted(name, fee, vehicles));
        assertThat(r.getResponse().getStatus()).as(json(r)).isEqualTo(201);
        return JsonPath.read(json(r), "$.id");
    }

    private void ensureCashOpen(MockHttpSession session) throws Exception {
        if (mockMvc.perform(get("/api/cash-sessions/current").session(session)).andReturn().getResponse().getStatus() == 404) {
            assertThat(postJson(session, "/api/cash-sessions/open", "{\"initialBalance\":0}").getResponse().getStatus())
                    .isEqualTo(201);
        }
    }

    private String enter(MockHttpSession session, String plate, String vehicleType) throws Exception {
        MvcResult r = postJson(session, "/api/parking-sessions",
                "{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, vehicleType));
        assertThat(r.getResponse().getStatus()).as(json(r)).isEqualTo(201);
        return json(r);
    }

    @Test
    void gestionSoloAdministradorYPlacaUnicaEntreMensualidades() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String id = createSubscription(admin, "Cliente Uno", 90000, "MEN101", "MEN102");

        mockMvc.perform(get("/api/subscriptions/" + id).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIN_PAGO"))
                .andExpect(jsonPath("$.vehicles.length()").value(2));

        // La placa ya pertenece a otra mensualidad.
        MvcResult duplicate = postJson(admin, "/api/subscriptions",
                "{\"customerName\":\"Otro\",\"monthlyFee\":1000,\"cutOffDay\":5,\"vehicles\":[{\"plate\":\"men 101\",\"vehicleTypeId\":\"%s\"}]}"
                        .formatted(CAR));
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(409);

        MvcResult badDay = postJson(admin, "/api/subscriptions",
                "{\"customerName\":\"Otro\",\"monthlyFee\":1000,\"cutOffDay\":31,\"vehicles\":[{\"plate\":\"MEN199\",\"vehicleTypeId\":\"%s\"}]}"
                        .formatted(CAR));
        assertThat(badDay.getResponse().getStatus()).isEqualTo(400);

        MockHttpSession employee = loginAs("empleado", "empleado123");
        mockMvc.perform(get("/api/subscriptions").session(employee)).andExpect(status().isOk());
        assertThat(postJson(employee, "/api/subscriptions", "{}").getResponse().getStatus()).isEqualTo(403);

        mockMvc.perform(get("/api/audit-logs").param("entity", "Subscription").session(admin))
                .andExpect(jsonPath("$[*].action").value(hasItem("SUBSCRIPTION_CREATED")));
    }

    @Test
    void pagoEnCajaIngresoReconocidoYSalidaSinCobro() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String id = createSubscription(admin, "Cliente Dos", 120000, "MEN201", "MEN202");
        ensureCashOpen(admin);
        String cashId = JsonPath.read(json(mockMvc.perform(get("/api/cash-sessions/current").session(admin)).andReturn()), "$.id");
        Number cashBefore = JsonPath.read(json(mockMvc.perform(get("/api/cash-sessions/" + cashId + "/summary")
                .session(admin)).andReturn()), "$.cashCollected");
        Number dashboardBefore = JsonPath.read(json(mockMvc.perform(get("/api/dashboard").session(admin)).andReturn()),
                "$.collectedToday");

        // Valor distinto al pactado -> rechazo.
        MvcResult wrong = postJson(admin, "/api/subscriptions/" + id + "/payments",
                "{\"paymentMethodId\":\"%s\",\"amount\":1000}".formatted(CASH));
        assertThat(json(wrong)).contains("CHARGED_AMOUNT_MISMATCH");

        MvcResult paid = postJson(admin, "/api/subscriptions/" + id + "/payments",
                "{\"paymentMethodId\":\"%s\",\"amount\":120000}".formatted(CASH));
        assertThat(paid.getResponse().getStatus()).as(json(paid)).isEqualTo(201);
        String paymentId = JsonPath.read(json(paid), "$.id");
        assertThat((String) JsonPath.read(json(paid), "$.operationNumber")).matches("OP-\\d{6}");
        assertThat((String) JsonPath.read(json(paid), "$.periodFrom")).isEqualTo(TODAY);

        mockMvc.perform(get("/api/subscriptions/" + id).session(admin))
                .andExpect(jsonPath("$.status").value("AL_DIA"));

        // Suma en el arqueo de la caja y aparece en las operaciones del turno.
        Number cashAfter = JsonPath.read(json(mockMvc.perform(get("/api/cash-sessions/" + cashId + "/summary")
                .session(admin)).andReturn()), "$.cashCollected");
        assertThat(cashAfter.doubleValue() - cashBefore.doubleValue()).isEqualTo(120000.0);
        // El Dashboard ("Recaudo hoy") incluye la mensualidad cobrada hoy.
        Number dashboardAfter = JsonPath.read(json(mockMvc.perform(get("/api/dashboard").session(admin)).andReturn()),
                "$.collectedToday");
        assertThat(dashboardAfter.doubleValue() - dashboardBefore.doubleValue()).isEqualTo(120000.0);
        mockMvc.perform(get("/api/cash-sessions/" + cashId + "/payments").session(admin))
                .andExpect(jsonPath("$[*].kind").value(hasItem("SUBSCRIPTION")));

        mockMvc.perform(get("/api/subscription-payments/" + paymentId + "/receipt").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Cliente Dos"))
                .andExpect(jsonPath("$.plates.length()").value(2));

        // El reporte del día incluye la mensualidad.
        mockMvc.perform(get("/api/reports/summary").param("from", TODAY).param("to", TODAY).session(admin))
                .andExpect(jsonPath("$.subscriptionPayments").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Ingreso con placa autorizada: se reconoce como mensual.
        String entry = enter(admin, "men 201", CAR);
        String sessionId = JsonPath.read(entry, "$.id");
        assertThat((String) JsonPath.read(entry, "$.subscriptionId")).isEqualTo(id);
        assertThat((String) JsonPath.read(entry, "$.notice")).contains("Cliente Dos");

        // Otro vehículo de la misma mensualidad mientras el primero está dentro: cliente normal con aviso.
        String second = enter(admin, "MEN202", MOTO);
        assertThat((Object) JsonPath.read(second, "$.subscriptionId")).isNull();
        assertThat((String) JsonPath.read(second, "$.notice")).contains("MEN201");

        // Salida cubierta: la cotización lo indica y el cobro normal se rechaza.
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/quote")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.covered").value(true))
                .andExpect(jsonPath("$.calculatedAmount").value(0));
        MvcResult normalCheckout = postJson(admin, "/api/parking-sessions/" + sessionId + "/checkout",
                "{\"paymentMethodId\":\"%s\",\"chargedAmount\":0}".formatted(CASH));
        assertThat(json(normalCheckout)).contains("SUBSCRIPTION_COVERED");

        MvcResult exit = mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/exit-subscription")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)).andReturn();
        assertThat(exit.getResponse().getStatus()).as(json(exit)).isEqualTo(200);
        assertThat((String) JsonPath.read(json(exit), "$.status")).isEqualTo("FINALIZADO");

        mockMvc.perform(get("/api/parking-sessions").param("from", TODAY).param("to", TODAY)
                        .param("plate", "MEN201").session(admin))
                .andExpect(jsonPath("$.items[0].subscriptionCustomerName").value("Cliente Dos"))
                .andExpect(jsonPath("$.items[0].paymentId").doesNotExist());
    }

    @Test
    void sinPagoOInactivaLaSalidaSeCobraNormal() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String id = createSubscription(admin, "Cliente Tres", 50000, "MEN301");

        String sessionId = JsonPath.read(enter(admin, "MEN301", CAR), "$.id");
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/quote")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin))
                .andExpect(jsonPath("$.subscription.status").value("SIN_PAGO"))
                .andExpect(jsonPath("$.subscription.covered").value(false))
                .andExpect(jsonPath("$.tariffId").isNotEmpty());
        MvcResult exit = mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/exit-subscription")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)).andReturn();
        assertThat(json(exit)).contains("SUBSCRIPTION_NOT_COVERING");

        // Desactivada: la placa ya no se reconoce al ingresar.
        mockMvc.perform(patch("/api/subscriptions/" + id + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVA"));
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/cancel")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"prueba\"}"));
        assertThat((Object) JsonPath.read(enter(admin, "MEN301", CAR), "$.subscriptionId")).isNull();
    }

    @Test
    void elPagoExigeCajaAbierta() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String id = createSubscription(admin, "Cliente Cuatro", 40000, "MEN401");
        MockHttpSession employee = loginAs("empleado", "empleado123");
        if (mockMvc.perform(get("/api/cash-sessions/current").session(employee)).andReturn().getResponse().getStatus() == 200) {
            return; // otra prueba dejó abierta la caja del empleado; el caso sin caja no aplica aquí.
        }
        MvcResult r = postJson(employee, "/api/subscriptions/" + id + "/payments",
                "{\"paymentMethodId\":\"%s\",\"amount\":40000}".formatted(CASH));
        assertThat(json(r)).contains("NO_OPEN_CASH_SESSION");
        List<?> payments = JsonPath.read(json(mockMvc.perform(get("/api/subscriptions/" + id + "/payments")
                .session(admin)).andReturn()), "$");
        assertThat(payments).isEmpty();
    }
}
