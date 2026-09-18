package com.barberbooking.api.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceResponse(
    UUID id, String name, String description, Integer durationMinutes, BigDecimal price, boolean active
) {
    static ServiceResponse from(Service service) {
        return new ServiceResponse(
            service.getId(), service.getName(), service.getDescription(),
            service.getDurationMinutes(), service.getPrice(), service.isActive()
        );
    }
}
