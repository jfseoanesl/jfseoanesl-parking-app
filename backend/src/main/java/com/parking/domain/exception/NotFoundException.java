package com.parking.domain.exception;

/** Entidad de dominio no encontrada (404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
