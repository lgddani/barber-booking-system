package com.barberbooking.api.appointments;

import com.barberbooking.api.availability.AvailabilityService;
import com.barberbooking.api.common.BusinessRuleException;
import com.barberbooking.api.common.ResourceNotFoundException;
import com.barberbooking.api.common.SlotNotAvailableException;
import com.barberbooking.api.security.UserPrincipal;
import com.barberbooking.api.users.BarberProfileRepository;
import com.barberbooking.api.users.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final List<AppointmentStatus> INACTIVE_STATUSES =
        List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = Map.of(
        AppointmentStatus.PENDING, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
        AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
    );

    private final AppointmentRepository appointmentRepository;
    private final com.barberbooking.api.catalog.ServiceRepository serviceRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    @Transactional
    public AppointmentResponse create(UUID customerId, AppointmentCreateRequest request) {
        if (!barberProfileRepository.existsById(request.barberId())) {
            throw new ResourceNotFoundException("Barbero no encontrado: " + request.barberId());
        }
        var service = serviceRepository.findById(request.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + request.serviceId()));

        // Reutiliza el mismo motor que pinta la grilla ✅/❌: cubre de una vez
        // horario laboral, excepciones, horas pasadas y citas ya existentes
        // del barbero. Esto es solo la validación "amigable" — la garantía
        // real contra condiciones de carrera es el EXCLUDE constraint de la
        // base de datos, más abajo.
        boolean slotOffered = availabilityService.getAvailableSlots(request.barberId(), request.date(), request.serviceId())
            .stream()
            .anyMatch(slot -> slot.time().equals(request.startTime()) && slot.available());
        if (!slotOffered) {
            throw new SlotNotAvailableException();
        }

        ZoneId zone = clock.getZone();
        Instant startAt = request.date().atTime(request.startTime()).atZone(zone).toInstant();
        Instant endAt = startAt.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);

        boolean customerBusy = !appointmentRepository
            .findActiveForCustomerInRange(customerId, startAt, endAt, INACTIVE_STATUSES)
            .isEmpty();
        if (customerBusy) {
            throw new BusinessRuleException("Ya tienes otra cita reservada en ese horario");
        }

        Appointment appointment = new Appointment();
        appointment.setCustomerId(customerId);
        appointment.setBarberId(request.barberId());
        appointment.setServiceId(request.serviceId());
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setPriceAtBooking(service.getPrice());

        try {
            // saveAndFlush (no save) a propósito: fuerza el INSERT a
            // ejecutarse ya, dentro de este try/catch. Si solo usáramos
            // save(), Hibernate podría diferir el INSERT hasta el commit —
            // y para entonces ya habríamos salido del catch, dejando pasar
            // la violación del constraint como un 500 en vez de un 409.
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            // La carrera real: dos peticiones pasaron el chequeo de arriba
            // casi al mismo tiempo, sobre el mismo horario. Postgres solo
            // deja pasar un INSERT; el otro cae aquí.
            throw new SlotNotAvailableException();
        }

        return AppointmentResponse.from(appointment);
    }

    public List<AppointmentResponse> listForUser(UserPrincipal principal, AppointmentStatus status) {
        List<Appointment> appointments = switch (principal.getRole()) {
            case ADMIN -> status != null
                ? appointmentRepository.findByStatusOrderByStartAtAsc(status)
                : appointmentRepository.findAllByOrderByStartAtAsc();
            case BARBER -> status != null
                ? appointmentRepository.findByBarberIdAndStatusOrderByStartAtAsc(principal.getId(), status)
                : appointmentRepository.findByBarberIdOrderByStartAtAsc(principal.getId());
            case CUSTOMER -> status != null
                ? appointmentRepository.findByCustomerIdAndStatusOrderByStartAtAsc(principal.getId(), status)
                : appointmentRepository.findByCustomerIdOrderByStartAtAsc(principal.getId());
        };
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    public AppointmentResponse getForUser(UUID id, UserPrincipal principal) {
        Appointment appointment = findOrThrow(id);
        requireOwnerOrAdmin(appointment, principal, true);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(UUID id, UserPrincipal principal) {
        Appointment appointment = findOrThrow(id);
        requireOwnerOrAdmin(appointment, principal, true);
        transition(appointment, AppointmentStatus.CANCELLED);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse complete(UUID id, UserPrincipal principal) {
        Appointment appointment = findOrThrow(id);
        requireOwnerOrAdmin(appointment, principal, false);
        transition(appointment, AppointmentStatus.COMPLETED);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse noShow(UUID id, UserPrincipal principal) {
        Appointment appointment = findOrThrow(id);
        requireOwnerOrAdmin(appointment, principal, false);
        transition(appointment, AppointmentStatus.NO_SHOW);
        return AppointmentResponse.from(appointment);
    }

    // includeCustomer=true deja pasar también al cliente dueño (ver/cancelar
    // su propia cita); false la restringe al barbero dueño o admin —
    // completar o marcar no-show es decisión del barbero, no del cliente.
    private void requireOwnerOrAdmin(Appointment appointment, UserPrincipal principal, boolean includeCustomer) {
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        boolean isOwnerBarber = appointment.getBarberId().equals(principal.getId());
        boolean isOwnerCustomer = includeCustomer && appointment.getCustomerId().equals(principal.getId());
        if (!isOwnerBarber && !isOwnerCustomer) {
            throw new AccessDeniedException("No tienes permiso para esta acción");
        }
    }

    private void transition(Appointment appointment, AppointmentStatus target) {
        Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(appointment.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessRuleException("No se puede pasar de " + appointment.getStatus() + " a " + target);
        }
        appointment.setStatus(target);
    }

    private Appointment findOrThrow(UUID id) {
        return appointmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + id));
    }
}
