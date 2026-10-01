package com.parking.infrastructure.error;

import java.time.Instant;

/** Formato uniforme de error (10-security-specification.md §Manejo centralizado de excepciones). */
public record ApiError(String error, String message, Instant timestamp) {

    public static ApiError of(String error, String message) {
        return new ApiError(error, message, Instant.now());
    }
}
