package com.parking.application.audit;

/** Utilidades para armar los valores JSON de auditoria (BR-025) sin romper el JSON con comillas. */
public final class AuditJson {

    private AuditJson() {
    }

    /** Cadena JSON escapada, o {@code null} literal. */
    public static String str(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Numero (o cualquier valor con toString numerico), o {@code null} literal. */
    public static String num(Object value) {
        return value == null ? "null" : value.toString();
    }
}
