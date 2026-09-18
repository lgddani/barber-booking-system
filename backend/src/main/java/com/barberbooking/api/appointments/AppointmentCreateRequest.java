package com.barberbooking.api.appointments;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// date + startTime en vez de un Instant: son exactamente lo que devuelve
// /availability, así que el frontend reenvía el mismo par que el usuario
// eligió de la grilla, sin tener que calcular zonas horarias por su cuenta.
public record AppointmentCreateRequest(
    @NotNull UUID barberId,
    @NotNull UUID serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime
) {
}
