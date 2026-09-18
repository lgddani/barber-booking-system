package com.barberbooking.api.barbers;

import java.util.UUID;

public record BarberResponse(UUID id, String fullName, String bio, boolean active) {
}
