package com.parking.domain.model;

/**
 * Codigos de rol semilla (prompt.md §3). Role vive como tabla (no enum) para permitir
 * roles adicionales en el futuro sin migracion de esquema; estos codigos son los unicos
 * que la logica de autorizacion del MVP reconoce (ver BR-003, 10-security-specification.md).
 */
public final class RoleCode {

    public static final String ADMIN = "ADMIN";
    public static final String EMPLOYEE = "EMPLOYEE";

    private RoleCode() {
    }
}
