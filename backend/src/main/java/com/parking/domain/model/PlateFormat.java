package com.parking.domain.model;

import com.parking.domain.exception.BusinessRuleViolationException;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * BR-036: la placa debe tener el formato del Ministerio de Transporte de Colombia para su tipo de
 * vehículo. Automóvil (CAR): tres letras y tres números (ABC123). Motocicleta (MOTORCYCLE): tres
 * letras, dos números y una letra (ABC12D), o la serie antigua de tres letras y dos números (ABC12).
 * Los tipos creados por el administrador con otro código solo pasan la validación laxa de BR-002.
 * Recibe placas ya normalizadas (mayúsculas, sin espacios).
 */
public final class PlateFormat {

    public static final String CAR = "CAR";
    public static final String MOTORCYCLE = "MOTORCYCLE";

    private static final Pattern CAR_PATTERN = Pattern.compile("[A-Z]{3}\\d{3}");
    private static final Pattern MOTORCYCLE_PATTERN = Pattern.compile("[A-Z]{3}\\d{2}[A-Z]?");

    private PlateFormat() {
    }

    /** Tipo (CAR o MOTORCYCLE) cuyo formato coincide con la placa, si alguno. */
    public static Optional<String> detect(String plate) {
        if (CAR_PATTERN.matcher(plate).matches()) return Optional.of(CAR);
        if (MOTORCYCLE_PATTERN.matcher(plate).matches()) return Optional.of(MOTORCYCLE);
        return Optional.empty();
    }

    /** Lanza PLATE_FORMAT_MISMATCH si la placa no corresponde al formato del tipo de vehículo. */
    public static void validate(String plate, String vehicleTypeCode) {
        if (!CAR.equals(vehicleTypeCode) && !MOTORCYCLE.equals(vehicleTypeCode)) {
            return;
        }
        Optional<String> detected = detect(plate);
        if (detected.filter(vehicleTypeCode::equals).isPresent()) {
            return;
        }
        String message = detected.isPresent()
                ? "La placa " + plate + " tiene formato de " + label(detected.get()) + ", no de " + label(vehicleTypeCode)
                : "La placa " + plate + " no tiene el formato de " + label(vehicleTypeCode) + " (" + example(vehicleTypeCode) + ")";
        throw new BusinessRuleViolationException("PLATE_FORMAT_MISMATCH", message);
    }

    private static String label(String code) {
        return CAR.equals(code) ? "automóvil" : "motocicleta";
    }

    private static String example(String code) {
        return CAR.equals(code) ? "ABC123" : "ABC12D o ABC12";
    }
}
