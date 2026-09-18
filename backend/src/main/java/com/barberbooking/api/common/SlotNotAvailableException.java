package com.barberbooking.api.common;

public class SlotNotAvailableException extends RuntimeException {

    public SlotNotAvailableException() {
        super("El horario solicitado ya no está disponible");
    }
}
