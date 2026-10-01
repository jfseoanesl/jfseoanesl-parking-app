package com.parking.domain.tariffengine;

import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.ParkingStatus;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffModality;
import com.parking.domain.model.TariffRounding;
import com.parking.domain.model.TariffTimeUnit;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bateria de pruebas de borde del motor tarifario (prompt.md §32): AM/PM, cruce de medianoche,
 * fracciones/tolerancia (delegadas a ByTimeAmountCalculatorTest), prioridad/empates, >24h,
 * y ausencia de tarifa configurada.
 */
class TariffCalculationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final String VEHICLE_TYPE_ID = "vt-car";

    private TariffRepository tariffRepository;
    private TariffCalculationService service;

    @BeforeEach
    void setUp() {
        tariffRepository = mock(TariffRepository.class);
        TimeProvider timeProvider = new TimeProvider() {
            @Override
            public Instant now() {
                return Instant.now();
            }

            @Override
            public ZoneId zone() {
                return ZONE;
            }
        };
        TariffStrategyFactory factory = new TariffStrategyFactory(
                new EntryWindowFixedTariffStrategy(timeProvider),
                new ExitWindowFixedTariffStrategy(timeProvider),
                new ByTimeTariffStrategy());
        service = new TariffCalculationService(tariffRepository, factory, timeProvider);
    }

    private static Instant at(int hour, int minute) {
        return ZonedDateTime.of(2026, 3, 10, hour, minute, 0, 0, ZONE).toInstant();
    }

    private static ParkingSession sessionEnteringAt(Instant entry) {
        return new ParkingSession("s1", "ABC123", VEHICLE_TYPE_ID, entry, null,
                ParkingStatus.EN_PARQUEADERO, "emp1", null, null, null, null, null, null, null,
                entry, entry);
    }

    private static Tariff tariff(String id, TariffModality modality, long value, LocalTime start, LocalTime end,
                                  int priority) {
        return new Tariff(id, id, null, VEHICLE_TYPE_ID, modality, BigDecimal.valueOf(value), start, end,
                10, null, TariffTimeUnit.MINUTES, 60, TariffRounding.UP, priority, true,
                LocalDate.of(2026, 1, 1), null, Instant.now(), Instant.now(), null, null);
    }

    private void givenTariffs(Tariff... tariffs) {
        when(tariffRepository.findActiveForVehicleType(anyString(), any())).thenReturn(List.of(tariffs));
    }

    @Test
    void tarifaTodoElDiaAplicaSegunHoraDeIngreso() {
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 8000,
                LocalTime.of(6, 0), LocalTime.of(22, 0), 1);
        givenTariffs(fullDay);

        ParkingSession session = sessionEnteringAt(at(8, 0)); // ingreso 08:00, dentro de la franja
        TariffCalculationResult result = service.calculate(session, at(10, 0));

        assertEquals(BigDecimal.valueOf(8000), result.amount());
        assertEquals(TariffModality.FULL_DAY, result.modality());
    }

    @Test
    void tarifaAmPmAplicaSegunHoraDeSalidaNoDeIngreso() {
        // Ingreso a las 17:00 (fuera de la franja AM), salida a las 08:00 del dia siguiente
        // (dentro de la franja AM 06:00-18:00) -> BR-014: gana la franja vigente en la salida.
        Tariff am = tariff("am", TariffModality.FULL_AM, 5000, LocalTime.of(6, 0), LocalTime.of(18, 0), 1);
        givenTariffs(am);

        ParkingSession session = sessionEnteringAt(
                ZonedDateTime.of(2026, 3, 10, 17, 0, 0, 0, ZONE).toInstant());
        Instant exit = ZonedDateTime.of(2026, 3, 11, 8, 0, 0, 0, ZONE).toInstant();

        TariffCalculationResult result = service.calculate(session, exit);

        assertEquals(BigDecimal.valueOf(5000), result.amount());
    }

    @Test
    void tarifaPmCruzaMedianocheCorrectamente() {
        Tariff pm = tariff("pm", TariffModality.FULL_PM, 6000, LocalTime.of(18, 0), LocalTime.of(6, 0), 1);
        givenTariffs(pm);

        ParkingSession session = sessionEnteringAt(at(19, 0));
        Instant exit = ZonedDateTime.of(2026, 3, 11, 2, 0, 0, 0, ZONE).toInstant(); // 02:00, dentro de 18:00-06:00

        TariffCalculationResult result = service.calculate(session, exit);

        assertEquals(BigDecimal.valueOf(6000), result.amount());
    }

    @Test
    void entrePrioridadesDistintasGanaLaDeMenorNumero() {
        Tariff alta = tariff("alta", TariffModality.FULL_DAY, 8000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff baja = tariff("baja", TariffModality.BY_TIME, 999999, null, null, 5);
        givenTariffs(alta, baja);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        TariffCalculationResult result = service.calculate(session, at(9, 0));

        assertEquals("alta", result.tariffId());
        assertEquals(BigDecimal.valueOf(8000), result.amount());
    }

    @Test
    void enEmpateDePrioridadGanaElMenorCobro_mecanismoDeBR013() {
        // Ambas con prioridad 1 (mismo tipo de vehiculo): el motor compara costo y elige el menor
        // (BR-008), lo que en la practica implementa el "tope" opcional de BR-013 sin campo extra.
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 5000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 500, null, null, 1);
        givenTariffs(fullDay, byTime);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        // 70 minutos por tiempo: 2 fracciones de 60min x 500 = 1000, mas barato que 5000 fijo.
        TariffCalculationResult result = service.calculate(session, at(9, 10));

        assertEquals("by-time", result.tariffId());
        assertEquals(BigDecimal.valueOf(1000), result.amount());
    }

    @Test
    void permanenciaMayorA24HorasCobraDiasCompletosMasRemanente() {
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 8000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 1000, null, null, 2);
        givenTariffs(fullDay, byTime);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        // 30 horas = 1 dia completo (24h) + 6h (360 min) de remanente.
        Instant exit = at(8, 0).plusSeconds(30L * 3600);

        TariffCalculationResult result = service.calculate(session, exit);

        // 1 dia = 8000; remanente 360 min / 60 = 6 fracciones x 1000 = 6000 -> total 14000.
        assertEquals(BigDecimal.valueOf(14000), result.amount());
    }

    @Test
    void permanenciaMayorA24HorasSinTarifaTodoElDiaUsaSoloPorTiempo() {
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 1000, null, null, 1);
        givenTariffs(byTime);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        Instant exit = at(8, 0).plusSeconds(25L * 3600); // 25 horas = 1500 minutos

        TariffCalculationResult result = service.calculate(session, exit);

        assertEquals(TariffModality.BY_TIME, result.modality());
        assertEquals(BigDecimal.valueOf(25000), result.amount()); // 1500/60 = 25 fracciones x 1000
    }

    @Test
    void sinTarifasConfiguradasLanzaExcepcionDeNegocio() {
        givenTariffs();

        ParkingSession session = sessionEnteringAt(at(8, 0));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.calculate(session, at(9, 0)));
        assertEquals("NO_TARIFF_CONFIGURED", ex.getCode());
    }

    @Test
    void sinTarifaAplicableParaElHorarioLanzaExcepcion() {
        // Solo hay tarifa AM (06:00-12:00) pero la salida es a las 20:00 -> ninguna aplica.
        Tariff am = tariff("am", TariffModality.FULL_AM, 5000, LocalTime.of(6, 0), LocalTime.of(12, 0), 1);
        givenTariffs(am);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.calculate(session, at(20, 0)));
        assertEquals("NO_APPLICABLE_TARIFF", ex.getCode());
    }

    // --- BR-008.1: tarifas alternas al cobrar ---

    @Test
    void opcionesListanLasAplicablesOrdenadasPorBr008ConLaSugeridaPrimero() {
        Tariff alta = tariff("alta", TariffModality.FULL_DAY, 8000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff baja = tariff("baja", TariffModality.BY_TIME, 1000, null, null, 5);
        Tariff am = tariff("am", TariffModality.FULL_AM, 5000, LocalTime.of(6, 0), LocalTime.of(12, 0), 3);
        givenTariffs(baja, am, alta);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        List<TariffCalculationResult> options = service.options(session, at(9, 0));

        assertEquals(List.of("alta", "am", "baja"), options.stream().map(TariffCalculationResult::tariffId).toList());
        assertEquals(service.calculate(session, at(9, 0)), options.get(0));
    }

    @Test
    void opcionesExcluyenTarifasCuyaFranjaNoAplica() {
        // Salida 20:00: la tarifa AM (06:00-12:00) no aplica y no debe ofrecerse como alterna.
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 1000, null, null, 1);
        Tariff am = tariff("am", TariffModality.FULL_AM, 5000, LocalTime.of(6, 0), LocalTime.of(12, 0), 2);
        givenTariffs(byTime, am);

        List<TariffCalculationResult> options = service.options(sessionEnteringAt(at(19, 0)), at(20, 0));

        assertEquals(List.of("by-time"), options.stream().map(TariffCalculationResult::tariffId).toList());
    }

    @Test
    void calcularConTarifaElegidaUsaSuPropioValor() {
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 5000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 500, null, null, 2);
        givenTariffs(fullDay, byTime);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        // Sugerida: full-day (prioridad 1) = 5000. Elegida: por tiempo, 70 min = 2 fracciones x 500.
        TariffCalculationResult chosen = service.calculateWith(session, at(9, 10), "by-time");

        assertEquals("by-time", chosen.tariffId());
        assertEquals(BigDecimal.valueOf(1000), chosen.amount());
    }

    @Test
    void calcularSinTarifaElegidaDevuelveLaSugerida() {
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 5000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 500, null, null, 2);
        givenTariffs(fullDay, byTime);

        TariffCalculationResult result = service.calculateWith(sessionEnteringAt(at(8, 0)), at(9, 10), null);

        assertEquals("full-day", result.tariffId());
    }

    @Test
    void calcularConTarifaNoAplicableSeRechaza() {
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 1000, null, null, 1);
        Tariff am = tariff("am", TariffModality.FULL_AM, 5000, LocalTime.of(6, 0), LocalTime.of(12, 0), 2);
        givenTariffs(byTime, am);

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.calculateWith(sessionEnteringAt(at(19, 0)), at(20, 0), "am"));
        assertEquals("TARIFF_NOT_APPLICABLE", ex.getCode());
    }

    @Test
    void permanenciaMayorA24HorasNoOfreceAlternasSoloElCalculoDeBr015() {
        Tariff fullDay = tariff("full-day", TariffModality.FULL_DAY, 8000, LocalTime.of(0, 0), LocalTime.of(0, 0), 1);
        Tariff byTime = tariff("by-time", TariffModality.BY_TIME, 1000, null, null, 2);
        givenTariffs(fullDay, byTime);

        ParkingSession session = sessionEnteringAt(at(8, 0));
        Instant exit = at(8, 0).plusSeconds(30L * 3600);

        List<TariffCalculationResult> options = service.options(session, exit);
        assertEquals(1, options.size());
        assertEquals(BigDecimal.valueOf(14000), options.get(0).amount());
        assertThrows(BusinessRuleViolationException.class, () -> service.calculateWith(session, exit, "by-time"));
    }
}
