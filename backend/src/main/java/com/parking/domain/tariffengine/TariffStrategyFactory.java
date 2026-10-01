package com.parking.domain.tariffengine;

import com.parking.domain.model.TariffModality;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Factory (decisions/ADR-003-motor-tarifario.md): resuelve la estrategia según la modalidad. */
@Component
public class TariffStrategyFactory {

    private final Map<TariffModality, TariffStrategy> strategiesByModality;

    public TariffStrategyFactory(EntryWindowFixedTariffStrategy entryWindowStrategy,
                                  ExitWindowFixedTariffStrategy exitWindowStrategy,
                                  ByTimeTariffStrategy byTimeStrategy) {
        this.strategiesByModality = Map.of(
                TariffModality.FULL_DAY, entryWindowStrategy,
                TariffModality.FULL_AM, exitWindowStrategy,
                TariffModality.FULL_PM, exitWindowStrategy,
                TariffModality.BY_TIME, byTimeStrategy
        );
    }

    public TariffStrategy forModality(TariffModality modality) {
        TariffStrategy strategy = strategiesByModality.get(modality);
        if (strategy == null) {
            throw new IllegalStateException("Modalidad de tarifa sin estrategia registrada: " + modality);
        }
        return strategy;
    }
}
