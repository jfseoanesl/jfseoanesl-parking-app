package com.parking.domain.tariffengine;

import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffRounding;

import java.math.BigDecimal;

/** BR-011 (fracciones/redondeo) y BR-012 (tolerancia/tiempo mínimo) para la modalidad BY_TIME. */
public final class ByTimeAmountCalculator {

    private ByTimeAmountCalculator() {
    }

    public static BigDecimal calculate(long elapsedMinutes, Tariff tariff) {
        int tolerance = tariff.toleranceMinutes() != null ? tariff.toleranceMinutes() : 0;
        if (elapsedMinutes <= tolerance) {
            return BigDecimal.ZERO;
        }

        long billableMinutes = elapsedMinutes;
        if (tariff.minTimeMinutes() != null && billableMinutes < tariff.minTimeMinutes()) {
            billableMinutes = tariff.minTimeMinutes();
        }

        int fractionSize = (tariff.fractionMinutes() != null && tariff.fractionMinutes() > 0)
                ? tariff.fractionMinutes() : 1;
        // Nota: con redondeo DOWN, una permanencia menor a una fraccion completa puede resultar
        // en 0 fracciones facturables (cobro $0) - es una eleccion deliberada del administrador
        // al configurar esa politica de redondeo (BR-011), no se fuerza un minimo de 1 fraccion.
        long fractions = computeFractions(billableMinutes, fractionSize, tariff.rounding());

        return tariff.value().multiply(BigDecimal.valueOf(fractions));
    }

    private static long computeFractions(long minutes, int fractionSize, TariffRounding rounding) {
        double raw = (double) minutes / fractionSize;
        return switch (rounding) {
            case DOWN -> (long) Math.floor(raw);
            case NEAREST -> Math.round(raw);
            case UP -> (long) Math.ceil(raw);
        };
    }
}
