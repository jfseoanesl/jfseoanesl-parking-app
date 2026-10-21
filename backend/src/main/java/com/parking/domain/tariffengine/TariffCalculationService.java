package com.parking.domain.tariffengine;

import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffModality;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta el calculo de tarifa (RF-043 a RF-048): resolucion normal por prioridad (BR-007,
 * BR-008) para permanencias de hasta 24h, y la regla especial de BR-015 para permanencias
 * mayores. Ver decisions/ADR-003-motor-tarifario.md.
 */
@Component
public class TariffCalculationService {

    private static final long MINUTES_PER_DAY = 24 * 60L;

    private final TariffRepository tariffRepository;
    private final TariffStrategyFactory strategyFactory;
    private final TimeProvider timeProvider;

    public TariffCalculationService(TariffRepository tariffRepository, TariffStrategyFactory strategyFactory,
                                     TimeProvider timeProvider) {
        this.tariffRepository = tariffRepository;
        this.strategyFactory = strategyFactory;
        this.timeProvider = timeProvider;
    }

    /** Tarifa sugerida por el motor (BR-008 / BR-015). */
    public TariffCalculationResult calculate(ParkingSession session, Instant referenceExit) {
        return options(session, referenceExit).get(0);
    }

    /**
     * BR-008.1: todas las tarifas aplicables a la sesion, ya calculadas y ordenadas por BR-008 (la
     * primera es la sugerida; el resto, las alternas que el usuario puede elegir al cobrar).
     * Para permanencias mayores a 24h solo hay una opcion: el calculo compuesto de BR-015.
     */
    public List<TariffCalculationResult> options(ParkingSession session, Instant referenceExit) {
        long elapsedMinutes = StayDurationCalculator.elapsedMinutes(session.entryDateTime(), referenceExit);
        LocalDate onDate = referenceExit.atZone(timeProvider.zone()).toLocalDate();
        List<Tariff> tariffs = tariffRepository.findActiveForVehicleType(session.vehicleTypeId(), onDate);

        if (tariffs.isEmpty()) {
            throw new BusinessRuleViolationException("NO_TARIFF_CONFIGURED",
                    "No hay tarifas configuradas para este tipo de vehículo");
        }

        if (elapsedMinutes > MINUTES_PER_DAY) {
            return List.of(calculateForLongStay(elapsedMinutes, tariffs));
        }
        return resolveByPriority(session, referenceExit, tariffs);
    }

    /**
     * BR-008.1: calcula con la tarifa elegida al cobrar (null = la sugerida). Solo se aceptan
     * tarifas aplicables a la sesion; el valor lo calcula siempre el motor, nunca el cliente.
     */
    public TariffCalculationResult calculateWith(ParkingSession session, Instant referenceExit, String tariffId) {
        List<TariffCalculationResult> options = options(session, referenceExit);
        if (tariffId == null || tariffId.isBlank()) {
            return options.get(0);
        }
        return options.stream()
                .filter(o -> o.tariffId().equals(tariffId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("TARIFF_NOT_APPLICABLE",
                        "La tarifa elegida no aplica a esta sesión"));
    }

    /** BR-008: entre las aplicables, gana la de menor `priority`; en empate, la de menor cobro. */
    private List<TariffCalculationResult> resolveByPriority(ParkingSession session, Instant referenceExit,
                                                        List<Tariff> tariffs) {
        record Candidate(Tariff tariff, TariffCalculationResult result) {
        }

        List<Candidate> candidates = tariffs.stream()
                .filter(t -> strategyFactory.forModality(t.modality()).isApplicable(session, t, referenceExit))
                .map(t -> new Candidate(t, strategyFactory.forModality(t.modality()).calculate(session, t, referenceExit)))
                .toList();

        if (candidates.isEmpty()) {
            throw new BusinessRuleViolationException("NO_APPLICABLE_TARIFF",
                    "No hay tarifa aplicable para el horario de esta sesión");
        }

        return candidates.stream()
                .sorted(Comparator.<Candidate>comparingInt(c -> c.tariff().priority())
                        .thenComparing(c -> c.result().amount()))
                .map(Candidate::result)
                .toList();
    }

    /** BR-015: dias completos a tarifa "todo el dia" + remanente por tiempo. */
    private TariffCalculationResult calculateForLongStay(long elapsedMinutes, List<Tariff> tariffs) {
        Optional<Tariff> fullDay = tariffs.stream()
                .filter(t -> t.modality() == TariffModality.FULL_DAY)
                .min(Comparator.comparingInt(Tariff::priority));
        Optional<Tariff> byTime = tariffs.stream()
                .filter(t -> t.modality() == TariffModality.BY_TIME)
                .min(Comparator.comparingInt(Tariff::priority));

        if (fullDay.isEmpty()) {
            if (byTime.isEmpty()) {
                throw new BusinessRuleViolationException("NO_TARIFF_CONFIGURED",
                        "No hay tarifa 'todo el día' ni 'por tiempo' para calcular una permanencia mayor a 24 horas");
            }
            Tariff tariff = byTime.get();
            BigDecimal amount = ByTimeAmountCalculator.calculate(elapsedMinutes, tariff);
            return new TariffCalculationResult(tariff.id(), tariff.name(), tariff.modality(), elapsedMinutes, amount);
        }

        Tariff dayTariff = fullDay.get();
        long days = elapsedMinutes / MINUTES_PER_DAY;
        long remainderMinutes = elapsedMinutes % MINUTES_PER_DAY;

        BigDecimal amount = dayTariff.value().multiply(BigDecimal.valueOf(days));
        if (remainderMinutes > 0) {
            amount = amount.add(byTime.map(t -> ByTimeAmountCalculator.calculate(remainderMinutes, t, false))
                    // Sin tarifa por tiempo para el remanente: se cobra un dia adicional (conservador,
                    // evita dejar el remanente sin cobrar).
                    .orElse(dayTariff.value()));
        }

        String label = dayTariff.name() + " × " + days + (remainderMinutes > 0 ? " + remanente" : "");
        return new TariffCalculationResult(dayTariff.id(), label, TariffModality.FULL_DAY, elapsedMinutes, amount);
    }
}
