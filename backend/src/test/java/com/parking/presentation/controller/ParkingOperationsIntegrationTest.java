package com.parking.presentation.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujos E2E prioritarios 1 y 2 de prompt.md §32: login -> ingreso -> vehiculo activo, y
 * buscar placa -> calcular tarifa -> pagar -> salida. Sobre SQLite en memoria con datos
 * semilla reales (usuarios, tipos de vehiculo, medios de pago, tarifas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingOperationsIntegrationTest {

    private static final String CAR_VEHICLE_TYPE_ID = "00000000-0000-0000-0000-000000000101";
    private static final String CASH_PAYMENT_METHOD_ID = "00000000-0000-0000-0000-000000000201";

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

    @Test
    void flujoCompletoIngresoSalidaCobro() throws Exception {
        MockHttpSession session = loginAs("empleado", "empleado123");

        // Abrir caja (prerequisito de Fase 5 para poder cobrar - ver CashSessionService).
        mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":50000}"))
                .andExpect(status().isCreated());

        // UC-002: registrar ingreso.
        MvcResult entryResult = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"zzz999\",\"vehicleTypeId\":\"%s\"}".formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("ZZZ999")) // BR-002: normalizada a mayusculas
                .andExpect(jsonPath("$.status").value("EN_PARQUEADERO"))
                .andReturn();
        String sessionId = com.jayway.jsonpath.JsonPath.read(entryResult.getResponse().getContentAsString(), "$.id");

        // UC-003: aparece en vehiculos activos.
        mockMvc.perform(get("/api/parking-sessions/active").param("plate", "ZZZ999").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plate").value("ZZZ999"));

        // BR-001: no permite un segundo ingreso activo para la misma placa.
        mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"ZZZ999\",\"vehicleTypeId\":\"%s\"}".formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_ACTIVE_PLATE"));

        // UC-005: cotizar sin registrar.
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/quote")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariffId").value("00000000-0000-0000-0000-000000000501"));

        // UC-004: registrar salida y cobro.
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":0}".formatted(CASH_PAYMENT_METHOD_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.operationNumber").exists());

        // Ya no aparece en activos.
        mockMvc.perform(get("/api/parking-sessions/active").param("plate", "ZZZ999").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // BR-021: no permite un segundo cobro sobre la misma sesion.
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":0}".formatted(CASH_PAYMENT_METHOD_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));
    }

    /** BR-008.1: al cobrar se puede elegir una tarifa alterna aplicable; el cambio queda auditado. */
    @Test
    void cobroConTarifaAlternaElegida() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");

        // Tarifa alterna para autos, siempre aplicable (por tiempo) pero con prioridad baja: nunca
        // es la sugerida, solo puede aplicarse si el usuario la elige.
        MvcResult tariffCreated = mockMvc.perform(post("/api/tariffs")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Convenio prueba\",\"vehicleTypeId\":\"%s\",\"modality\":\"BY_TIME\","
                                + "\"value\":700,\"fractionMinutes\":60,\"timeUnit\":\"MINUTES\",\"rounding\":\"UP\","
                                + "\"priority\":99,\"validFrom\":\"2026-01-01\"}").formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String alternateTariffId = com.jayway.jsonpath.JsonPath.read(
                tariffCreated.getResponse().getContentAsString(), "$.id");

        // Usuario propio para no interferir con la caja de "empleado"/"admin" en otros tests.
        mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cobradoralterno\",\"fullName\":\"Cobrador Alterno\","
                                + "\"password\":\"claveInicial1\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated());
        MockHttpSession session = loginAs("cobradoralterno", "claveInicial1");
        mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":0}"))
                .andExpect(status().isCreated());

        String sessionId = registerEntry(session, "ALT001");

        // La cotizacion ofrece la alterna junto a la sugerida (que va primero).
        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/quote")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].suggested").value(true))
                .andExpect(jsonPath("$.options[0].tariffId").value(org.hamcrest.Matchers.not(alternateTariffId)))
                .andExpect(jsonPath("$.options[?(@.tariffId=='%s')].suggested".formatted(alternateTariffId))
                        .value(org.hamcrest.Matchers.contains(false)));

        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":0,\"tariffId\":\"%s\"}"
                                .formatted(CASH_PAYMENT_METHOD_ID, alternateTariffId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedTariffName").value("Convenio prueba"));

        // BR-025: el cambio de tarifa queda en auditoria.
        mockMvc.perform(get("/api/audit-logs").param("entity", "ParkingSession").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entityId=='%s')].action".formatted(sessionId))
                        .value(org.hamcrest.Matchers.contains("TARIFF_OVERRIDDEN")));

        // Una tarifa que no aplica a la sesion se rechaza (el valor nunca lo decide el cliente).
        String otherSessionId = registerEntry(session, "ALT002");
        mockMvc.perform(post("/api/parking-sessions/" + otherSessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":0,\"tariffId\":\"no-existe\"}"
                                .formatted(CASH_PAYMENT_METHOD_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TARIFF_NOT_APPLICABLE"));

        // RF-037: el valor cobrado debe coincidir con el calculado; un monto inventado (o de una
        // cotizacion desactualizada) se rechaza sin registrar nada y la sesion sigue activa.
        mockMvc.perform(post("/api/parking-sessions/" + otherSessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":700}".formatted(CASH_PAYMENT_METHOD_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CHARGED_AMOUNT_MISMATCH"));
        mockMvc.perform(get("/api/parking-sessions/active").param("plate", "ALT002").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EN_PARQUEADERO"));
    }

    /**
     * RF-049: cobros simultaneos reciben numeros de operacion distintos. Regresion: con COUNT+1 dos
     * cobros concurrentes en MySQL obtenian el mismo numero y uno fallaba con 500. En SQLite el acceso
     * es serial y la prueba siempre pasa; la que detecta el bug es la corrida sobre MySQL
     * (scripts/verify-mysql.sh).
     */
    @Test
    void cobrosSimultaneosRecibenNumerosDeOperacionDistintos() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "admin123");
        mockMvc.perform(post("/api/users")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cobradorparalelo\",\"fullName\":\"Cobrador Paralelo\","
                                + "\"password\":\"claveInicial1\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated());
        MockHttpSession session = loginAs("cobradorparalelo", "claveInicial1");
        mockMvc.perform(post("/api/cash-sessions/open")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":0}"))
                .andExpect(status().isCreated());

        int count = 8;
        java.util.List<String> sessionIds = new java.util.ArrayList<>();
        java.util.List<String> amounts = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = registerEntry(session, "PAR00" + i);
            MvcResult quote = mockMvc.perform(post("/api/parking-sessions/" + id + "/quote")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            sessionIds.add(id);
            amounts.add(com.jayway.jsonpath.JsonPath.read(quote.getResponse().getContentAsString(), "$.calculatedAmount")
                    .toString());
        }

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(count);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<MvcResult>> results = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = sessionIds.get(i);
            String amount = amounts.get(i);
            results.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/parking-sessions/" + id + "/checkout")
                                .with(SecurityMockMvcRequestPostProcessors.csrf())
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":%s}"
                                        .formatted(CASH_PAYMENT_METHOD_ID, amount)))
                        .andReturn();
            }));
        }
        start.countDown();
        java.util.Set<String> operationNumbers = new java.util.HashSet<>();
        for (java.util.concurrent.Future<MvcResult> future : results) {
            MvcResult result = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus(),
                    result.getResponse().getContentAsString());
            operationNumbers.add(com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),
                    "$.operationNumber"));
        }
        pool.shutdown();
        org.junit.jupiter.api.Assertions.assertEquals(count, operationNumbers.size(), "numeros repetidos: " + operationNumbers);
    }

    private String registerEntry(MockHttpSession session, String plate) throws Exception {
        MvcResult entry = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"%s\",\"vehicleTypeId\":\"%s\"}".formatted(plate, CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(entry.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void checkoutSinCajaAbiertaFalla() throws Exception {
        // Empleado nuevo: nunca tiene caja abierta, sin importar qué pruebas corrieron antes.
        MockHttpSession session = freshEmployee();

        MvcResult entryResult = mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"YYY888\",\"vehicleTypeId\":\"%s\"}".formatted(CAR_VEHICLE_TYPE_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = com.jayway.jsonpath.JsonPath.read(entryResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/parking-sessions/" + sessionId + "/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethodId\":\"%s\",\"chargedAmount\":0}".formatted(CASH_PAYMENT_METHOD_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_OPEN_CASH_SESSION"));
    }

    @Test
    void registrarIngresoSinTipoDeVehiculoEsRechazado() throws Exception {
        MockHttpSession session = loginAs("admin", "admin123");

        mockMvc.perform(post("/api/parking-sessions")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate\":\"XXX777\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
