package com.barberbooking.api.appointments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID customerId,
    UUID barberId,
    UUID serviceId,
    Instant startAt,
    Instant endAt,
    AppointmentStatus status,
    BigDecimal priceAtBooking,
    String notes
) {
    static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
            a.getId(), a.getCustomerId(), a.getBarberId(), a.getServiceId(),
            a.getStartAt(), a.getEndAt(), a.getStatus(), a.getPriceAtBooking(), a.getNotes()
        );
    }
}
