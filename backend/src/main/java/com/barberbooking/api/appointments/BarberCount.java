package com.barberbooking.api.appointments;

import java.util.UUID;

public record BarberCount(UUID barberId, long count) {
}
