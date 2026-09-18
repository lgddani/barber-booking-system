package com.barberbooking.api.admin;

import com.barberbooking.api.appointments.AppointmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AdminStatsResponse(
    LocalDate from,
    LocalDate to,
    long totalAppointments,
    Map<AppointmentStatus, Long> byStatus,
    BigDecimal estimatedRevenue,
    BusiestBarber busiestBarber
) {
    public record BusiestBarber(UUID barberId, String fullName, long appointmentCount) {
    }
}
