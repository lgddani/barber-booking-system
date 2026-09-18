package com.barberbooking.api.common;

// Excepción genérica para reglas de negocio simples (p. ej. horarios mal
// formados). No cada regla necesita su propia clase de excepción.
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
