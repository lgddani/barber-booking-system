package com.barberbooking.api.appointments;

import com.barberbooking.api.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(
        @Valid @RequestBody AppointmentCreateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return appointmentService.create(principal.getId(), request);
    }

    @GetMapping
    public List<AppointmentResponse> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) AppointmentStatus status
    ) {
        return appointmentService.listForUser(principal, status);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.getForUser(id, principal);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.cancel(id, principal);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public AppointmentResponse complete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.complete(id, principal);
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BARBER')")
    public AppointmentResponse noShow(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.noShow(id, principal);
    }
}
