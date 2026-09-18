package com.barberbooking.api.barbers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/barbers/{barberId}/working-hours")
@RequiredArgsConstructor
public class WorkingHoursController {

    private final WorkingHoursService workingHoursService;

    @GetMapping
    public List<WorkingHoursItem> get(@PathVariable UUID barberId) {
        return workingHoursService.get(barberId);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or #barberId == authentication.principal.id")
    public List<WorkingHoursItem> replace(@PathVariable UUID barberId, @Valid @RequestBody List<WorkingHoursItem> items) {
        return workingHoursService.replace(barberId, items);
    }
}
