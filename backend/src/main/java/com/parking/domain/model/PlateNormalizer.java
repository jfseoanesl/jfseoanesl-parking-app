package com.parking.domain.model;

import java.util.regex.Pattern;

/** BR-002: normalización y validación laxa de placa (mayúsculas, sin espacios). */
public final class PlateNormalizer {

    private static final Pattern REASONABLE_FORMAT = Pattern.compile("[A-Z0-9]{3,10}");

    private PlateNormalizer() {
    }

    public static String normalize(String rawPlate) {
        if (rawPlate == null || rawPlate.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        String normalized = rawPlate.trim().toUpperCase().replaceAll("\\s+", "");
        if (!REASONABLE_FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Formato de placa no reconocido: " + rawPlate);
        }
        return normalized;
    }
}
