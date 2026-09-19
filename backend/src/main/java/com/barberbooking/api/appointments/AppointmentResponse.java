package com.barberbooking.api.appointments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID customerId,
    String customerName,
    UUID barberId,
    UUID serviceId,
    Instant startAt,
    Instant endAt,
    AppointmentStatus status,
    BigDecimal priceAtBooking,
    String notes,
    Instant createdAt
) {
    static AppointmentResponse from(Appointment a, String customerName) {
        return new AppointmentResponse(
            a.getId(), a.getCustomerId(), customerName, a.getBarberId(), a.getServiceId(),
            a.getStartAt(), a.getEndAt(), a.getStatus(), a.getPriceAtBooking(), a.getNotes(), a.getCreatedAt()
        );
    }
}
