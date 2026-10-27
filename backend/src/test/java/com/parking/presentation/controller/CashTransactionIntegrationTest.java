package com.parking.presentation.controller;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** UC-014 / RF-079 a RF-084 / BR-032 a BR-035: otros ingresos y gastos de caja de punta a punta. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CashTransactionIntegrationTest {

    private static final String CASH = "00000000-0000-0000-0000-000000000201";
    private static final String TRANSFER = "00000000-0000-0000-0000-000000000202";
    private static final String INCOME_CONCEPT = "00000000-0000-0000-0000-000000000501";
    private static final String EXPENSE_CONCEPT = "00000000-0000-0000-0000-000000000502";
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

    private MvcResult getJson(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(get(url).session(session)).andReturn();
    }

    /** Empleado propio de la prueba (no se deja abierta la caja de los usuarios del seed). */
    private MockHttpSession employee(MockHttpSession admin, boolean openCash, int initialBalance) throws Exception {
        String username = "caja" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult created = postJson(admin, "/api/users",
                "{\"username\":\"%s\",\"fullName\":\"%s\",\"password\":\"claveSegura1\",\"role\":\"EMPLOYEE\"}"
                        .formatted(username, username));
        assertThat(created.getResponse().getStatus()).as(json(created)).isEqualTo(201);
        MockHttpSession session = loginAs(username, "claveSegura1");
        if (openCash) {
            assertThat(postJson(session, "/api/cash-sessions/open", "{\"initialBalance\":%d}".formatted(initialBalance))
                    .getResponse().getStatus()).isEqualTo(201);
        }
        return session;
    }

    private String cashId(MockHttpSession session) throws Exception {
        return JsonPath.read(json(getJson(session, "/api/cash-sessions/current")), "$.id");
    }

    private MvcResult income(MockHttpSession session, String method, int amount, String description) throws Exception {
        return postJson(session, "/api/cash-transactions",
                "{\"type\":\"INCOME\",\"conceptId\":\"%s\",\"description\":\"%s\",\"amount\":%d,\"paymentMethodId\":\"%s\"}"
                        .formatted(INCOME_CONCEPT, description, amount, method));
    }

    private MvcResult expense(MockHttpSession session, String cashSessionId, String method, int amount) throws Exception {
        return postJson(session, "/api/cash-transactions",
                "{\"type\":\"EXPENSE\",\"conceptId\":\"%s\",\"description\":\"Papeleria\",\"amount\":%d,\"paymentMethodId\":\"%s\",\"cashSessionId\":\"%s\"}"
                        .formatted(EXPENSE_CONCEPT, amount, method, cashSessionId));
    }

    private static BigDecimal decimal(String json, String path) {
        return new BigDecimal(JsonPath.read(json, path).toString());
    }

    @Test
    void ingresosYGastosAfectanElArqueoYLaAnulacionLosRevierte() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        MockHttpSession cashier = employee(admin, true, 10000);
        String cashId = cashId(cashier);
        BigDecimal dashboardBefore = decimal(json(getJson(admin, "/api/dashboard")), "$.collectedToday");
        BigDecimal expensesBefore = decimal(json(getJson(admin, "/api/dashboard")), "$.expensesToday");

        // BR-032: el empleado registra otros ingresos en su caja abierta.
        MvcResult cashIncome = income(cashier, CASH, 5000, "Lavado");
        assertThat(cashIncome.getResponse().getStatus()).as(json(cashIncome)).isEqualTo(201);
        String cashIncomeOp = JsonPath.read(json(cashIncome), "$.operationNumber");
        assertThat(cashIncomeOp).startsWith("OP-");
        MvcResult transferIncome = income(cashier, TRANSFER, 3000, "Llaveros");
        assertThat(transferIncome.getResponse().getStatus()).isEqualTo(201);
        // BR-034: consecutivo OP- compartido y creciente.
        int first = Integer.parseInt(cashIncomeOp.substring(3));
        int second = Integer.parseInt(JsonPath.<String>read(json(transferIncome), "$.operationNumber").substring(3));
        assertThat(second).isGreaterThan(first);

        // El empleado no puede registrar gastos.
        assertThat(expense(cashier, cashId, CASH, 100).getResponse().getStatus()).isEqualTo(403);

        // El gasto en efectivo no puede superar el efectivo disponible (10000 + 5000).
        MvcResult tooMuch = expense(admin, cashId, CASH, 15001);
        assertThat(json(tooMuch)).contains("INSUFFICIENT_CASH");

        // El administrador registra un gasto en la caja del empleado.
        MvcResult cashExpense = expense(admin, cashId, CASH, 4000);
        assertThat(cashExpense.getResponse().getStatus()).as(json(cashExpense)).isEqualTo(201);
        String expenseId = JsonPath.read(json(cashExpense), "$.id");
        assertThat(expense(admin, cashId, TRANSFER, 1000).getResponse().getStatus()).isEqualTo(201);

        // BR-033: los ingresos suman por medio de pago y solo el gasto en efectivo sale del cajon.
        String summary = json(getJson(cashier, "/api/cash-sessions/" + cashId + "/summary"));
        assertThat(decimal(summary, "$.cashCollected")).isEqualByComparingTo("5000");
        assertThat(decimal(summary, "$.electronicCollected")).isEqualByComparingTo("3000");
        assertThat(decimal(summary, "$.otherIncomeCollected")).isEqualByComparingTo("8000");
        assertThat(decimal(summary, "$.expensesTotal")).isEqualByComparingTo("5000");
        assertThat(decimal(summary, "$.expensesCash")).isEqualByComparingTo("4000");
        assertThat(decimal(summary, "$.expectedCash")).isEqualByComparingTo("11000");

        // Operaciones del turno con los tipos nuevos.
        String operations = json(getJson(cashier, "/api/cash-sessions/" + cashId + "/payments"));
        List<String> kinds = JsonPath.read(operations, "$[*].kind");
        assertThat(kinds).contains("INCOME", "EXPENSE");

        // Dashboard: los ingresos son recaudo del dia; los gastos se muestran aparte.
        String dashboard = json(getJson(admin, "/api/dashboard"));
        assertThat(decimal(dashboard, "$.collectedToday").subtract(dashboardBefore)).isEqualByComparingTo("8000");
        assertThat(decimal(dashboard, "$.expensesToday").subtract(expensesBefore)).isEqualByComparingTo("5000");

        // BR-035: el empleado no puede anular; el administrador si, con motivo.
        assertThat(postJson(cashier, "/api/cash-transactions/" + expenseId + "/void", "{\"reason\":\"x\"}")
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(postJson(admin, "/api/cash-transactions/" + expenseId + "/void", "{\"reason\":\"\"}")
                .getResponse().getStatus()).isEqualTo(400);
        MvcResult voided = postJson(admin, "/api/cash-transactions/" + expenseId + "/void", "{\"reason\":\"Error de digitacion\"}");
        assertThat(voided.getResponse().getStatus()).as(json(voided)).isEqualTo(200);
        assertThat(JsonPath.<String>read(json(voided), "$.status")).isEqualTo("VOIDED");
        assertThat(json(postJson(admin, "/api/cash-transactions/" + expenseId + "/void", "{\"reason\":\"otra vez\"}")))
                .contains("CASH_TRANSACTION_ALREADY_VOIDED");
        assertThat(decimal(json(getJson(cashier, "/api/cash-sessions/" + cashId + "/summary")), "$.expectedCash"))
                .isEqualByComparingTo("15000");

        // Comprobante: lo ve el dueño de la caja.
        mockMvc.perform(get("/api/cash-transactions/" + expenseId + "/receipt").session(cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.type").value("EXPENSE"))
                .andExpect(jsonPath("$.transaction.conceptName").value("Gastos varios"))
                .andExpect(jsonPath("$.transaction.status").value("VOIDED"));
        MockHttpSession other = employee(admin, false, 0);
        mockMvc.perform(get("/api/cash-transactions/" + expenseId + "/receipt").session(other))
                .andExpect(status().isForbidden());

        // Cierre: la foto guarda otros ingresos y gastos activos.
        MvcResult closed = postJson(cashier, "/api/cash-sessions/" + cashId + "/close", "{\"reportedCash\":15000}");
        assertThat(closed.getResponse().getStatus()).as(json(closed)).isEqualTo(200);
        assertThat(decimal(json(closed), "$.otherIncomeCollected")).isEqualByComparingTo("8000");
        assertThat(decimal(json(closed), "$.expensesTotal")).isEqualByComparingTo("1000");
        assertThat(decimal(json(closed), "$.difference")).isEqualByComparingTo("0");

        // Con la caja cerrada ya no se puede anular ni registrar gastos en ella.
        String transferExpenseId = JsonPath.<List<String>>read(operations,
                "$[?(@.kind=='EXPENSE' && @.paymentMethodName=='Transferencia')].paymentId").get(0);
        assertThat(json(postJson(admin, "/api/cash-transactions/" + transferExpenseId + "/void", "{\"reason\":\"tarde\"}")))
                .contains("CASH_SESSION_CLOSED");
        assertThat(json(expense(admin, cashId, CASH, 100))).contains("CASH_SESSION_NOT_OPEN");

        mockMvc.perform(get("/api/audit-logs").param("entity", "CashTransaction").session(admin))
                .andExpect(jsonPath("$[*].action").value(hasItem("CASH_TRANSACTION_VOIDED")))
                .andExpect(jsonPath("$[*].action").value(hasItem("CASH_EXPENSE_REGISTERED")));
    }

    @Test
    void validacionesDeRegistro() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");

        // Sin caja abierta no se registran ingresos.
        MockHttpSession noCash = employee(admin, false, 0);
        assertThat(json(income(noCash, CASH, 1000, "x"))).contains("NO_OPEN_CASH_SESSION");

        MockHttpSession cashier = employee(admin, true, 0);
        // Concepto de otro tipo, valor invalido y gasto sin caja.
        assertThat(postJson(cashier, "/api/cash-transactions",
                "{\"type\":\"INCOME\",\"conceptId\":\"%s\",\"amount\":1000,\"paymentMethodId\":\"%s\"}"
                        .formatted(EXPENSE_CONCEPT, CASH)).getResponse().getStatus()).isEqualTo(400);
        assertThat(income(cashier, CASH, 0, "x").getResponse().getStatus()).isEqualTo(400);
        assertThat(postJson(admin, "/api/cash-transactions",
                "{\"type\":\"EXPENSE\",\"conceptId\":\"%s\",\"amount\":1000,\"paymentMethodId\":\"%s\"}"
                        .formatted(EXPENSE_CONCEPT, CASH)).getResponse().getStatus()).isEqualTo(400);

        // Un concepto desactivado ya no se puede usar.
        MvcResult concept = postJson(admin, "/api/cash-concepts",
                "{\"type\":\"INCOME\",\"name\":\"Concepto %s\"}".formatted(UUID.randomUUID().toString().substring(0, 6)));
        assertThat(concept.getResponse().getStatus()).as(json(concept)).isEqualTo(201);
        String conceptId = JsonPath.read(json(concept), "$.id");
        mockMvc.perform(patch("/api/cash-concepts/" + conceptId + "/status").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(admin).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertThat(postJson(cashier, "/api/cash-transactions",
                "{\"type\":\"INCOME\",\"conceptId\":\"%s\",\"amount\":1000,\"paymentMethodId\":\"%s\"}"
                        .formatted(conceptId, CASH)).getResponse().getStatus()).isEqualTo(400);

        // Cajas abiertas para elegir el gasto: solo administrador.
        String cashId = cashId(cashier);
        mockMvc.perform(get("/api/cash-sessions/open").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id").value(hasItem(cashId)));
        mockMvc.perform(get("/api/cash-sessions/open").session(cashier)).andExpect(status().isForbidden());

        assertThat(postJson(cashier, "/api/cash-sessions/" + cashId + "/close", "{\"reportedCash\":0}")
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void catalogoDeConceptosSoloAdministrador() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        MockHttpSession employee = loginAs("empleado", "empleado123");
        String name = "Parqueo de bicicletas " + UUID.randomUUID().toString().substring(0, 6);

        MvcResult created = postJson(admin, "/api/cash-concepts", "{\"type\":\"INCOME\",\"name\":\"%s\"}".formatted(name));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String id = JsonPath.read(json(created), "$.id");
        // Duplicado (sin distinguir mayusculas) dentro del mismo tipo.
        assertThat(postJson(admin, "/api/cash-concepts", "{\"type\":\"INCOME\",\"name\":\"%s\"}"
                .formatted(name.toUpperCase())).getResponse().getStatus()).isEqualTo(409);
        // El mismo nombre como gasto si se permite.
        assertThat(postJson(admin, "/api/cash-concepts", "{\"type\":\"EXPENSE\",\"name\":\"%s\"}".formatted(name))
                .getResponse().getStatus()).isEqualTo(201);

        mockMvc.perform(put("/api/cash-concepts/" + id).with(SecurityMockMvcRequestPostProcessors.csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"%s editado\"}".formatted(name)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name + " editado"));

        mockMvc.perform(get("/api/cash-concepts").param("type", "INCOME").param("active", "true").session(employee))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].name").value(hasItem("Otros ingresos")));
        assertThat(postJson(employee, "/api/cash-concepts", "{\"type\":\"INCOME\",\"name\":\"X\"}")
                .getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void reportesIncluyenOtrosIngresosGastosYNeto() throws Exception {
        MockHttpSession admin = loginAs("admin", "admin123");
        String general = "/api/reports/summary?from=%s&to=%s".formatted(TODAY, TODAY);
        String before = json(getJson(admin, general));

        MockHttpSession cashier = employee(admin, true, 50000);
        String cashId = cashId(cashier);
        String description = "Reporte " + UUID.randomUUID().toString().substring(0, 6);
        assertThat(income(cashier, CASH, 7000, description).getResponse().getStatus()).isEqualTo(201);
        assertThat(expense(admin, cashId, CASH, 2500).getResponse().getStatus()).isEqualTo(201);

        String after = json(getJson(admin, general));
        assertThat(decimal(after, "$.otherIncomeCollected").subtract(decimal(before, "$.otherIncomeCollected")))
                .isEqualByComparingTo("7000");
        assertThat(decimal(after, "$.expensesTotal").subtract(decimal(before, "$.expensesTotal")))
                .isEqualByComparingTo("2500");
        assertThat(decimal(after, "$.totalCollected").subtract(decimal(before, "$.totalCollected")))
                .isEqualByComparingTo("7000");
        assertThat(decimal(after, "$.netCollected"))
                .isEqualByComparingTo(decimal(after, "$.totalCollected").subtract(decimal(after, "$.expensesTotal")));

        // Reporte de ingresos y gastos filtrado por tipo.
        String report = json(getJson(admin, "/api/reports/cash-transactions?from=%s&to=%s&type=INCOME".formatted(TODAY, TODAY)));
        List<String> types = JsonPath.read(report, "$.transactions[*].type");
        assertThat(types).isNotEmpty().allMatch("INCOME"::equals);
        assertThat(JsonPath.<List<String>>read(report, "$.transactions[*].description")).contains(description);
        assertThat(getJson(admin, "/api/reports/cash-transactions?from=%s&to=%s&type=OTRO".formatted(TODAY, TODAY))
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(getJson(admin, "/api/reports/cash-transactions?from=%s&to=%s".formatted(TODAY, LocalDate.parse(TODAY).minusDays(1)))
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(getJson(cashier, "/api/reports/cash-transactions?from=%s&to=%s".formatted(TODAY, TODAY))
                .getResponse().getStatus()).isEqualTo(403);

        MvcResult xlsx = getJson(admin, "/api/reports/cash-transactions/export?from=%s&to=%s&format=xlsx".formatted(TODAY, TODAY));
        assertThat(xlsx.getResponse().getStatus()).isEqualTo(200);
        assertThat(xlsx.getResponse().getHeader("Content-Disposition")).contains("reporte-ingresos-gastos_" + TODAY + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getSheet("Conceptos")).isNotNull();
            assertThat(workbook.getSheet("Movimientos")).isNotNull();
        }
        MvcResult csv = getJson(admin, "/api/reports/cash-transactions/export?from=%s&to=%s&format=csv".formatted(TODAY, TODAY));
        assertThat(new String(csv.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8)).contains(description);
        assertThat(getJson(admin, "/api/reports/cash-transactions/export?from=%s&to=%s&format=pdf".formatted(TODAY, TODAY))
                .getResponse().getStatus()).isEqualTo(200);

        assertThat(postJson(cashier, "/api/cash-sessions/" + cashId + "/close", "{\"reportedCash\":54500}")
                .getResponse().getStatus()).isEqualTo(200);
    }
}
