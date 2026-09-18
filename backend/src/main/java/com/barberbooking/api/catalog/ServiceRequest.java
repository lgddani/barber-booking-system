package com.barberbooking.api.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ServiceRequest(
    @NotBlank @Size(max = 120) String name,
    String description,
    @NotNull @Positive Integer durationMinutes,
    @NotNull @DecimalMin(value = "0.0") BigDecimal price
) {
}
