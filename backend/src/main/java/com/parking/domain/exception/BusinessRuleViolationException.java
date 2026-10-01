package com.parking.domain.exception;

/** Violación de una regla de negocio (05-business-rules.md) que el cliente puede corregir. */
public class BusinessRuleViolationException extends RuntimeException {

    private final String code;

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
