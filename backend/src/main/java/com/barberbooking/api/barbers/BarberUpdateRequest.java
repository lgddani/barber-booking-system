package com.barberbooking.api.barbers;

import jakarta.validation.constraints.NotBlank;

public record BarberUpdateRequest(
    @NotBlank String fullName,
    String phone,
    String bio
) {
}
