package com.barberbooking.api.barbers;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/barbers/{barberId}/exceptions")
@RequiredArgsConstructor
public class ScheduleExceptionController {

    private final ScheduleExceptionService scheduleExceptionService;

    @GetMapping
    public List<ScheduleExceptionResponse> list(
        @PathVariable UUID barberId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return scheduleExceptionService.list(barberId, from, to);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #barberId == authentication.principal.id")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleExceptionResponse create(@PathVariable UUID barberId, @Valid @RequestBody ScheduleExceptionRequest request) {
        return scheduleExceptionService.create(barberId, request);
    }

    @DeleteMapping("/{exceptionId}")
    @PreAuthorize("hasRole('ADMIN') or #barberId == authentication.principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID barberId, @PathVariable UUID exceptionId) {
        scheduleExceptionService.delete(barberId, exceptionId);
    }
}
