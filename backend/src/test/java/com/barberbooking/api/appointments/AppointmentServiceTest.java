package com.barberbooking.api.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barberbooking.api.availability.AvailabilityService;
import com.barberbooking.api.availability.TimeSlot;
import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.catalog.ServiceRepository;
import com.barberbooking.api.common.BusinessRuleException;
import com.barberbooking.api.common.ResourceNotFoundException;
import com.barberbooking.api.common.SlotNotAvailableException;
import com.barberbooking.api.security.UserPrincipal;
import com.barberbooking.api.users.BarberProfileRepository;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import com.barberbooking.api.users.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final UUID BARBER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 9, 21);
    private static final LocalTime TIME = LocalTime.of(10, 0);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BarberProfileRepository barberProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private AvailabilityService availabilityService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private AppointmentService appointmentService() {
        return new AppointmentService(
            appointmentRepository, serviceRepository, barberProfileRepository, userRepository, availabilityService, clock
        );
    }

    private AppointmentCreateRequest request() {
        return new AppointmentCreateRequest(BARBER_ID, SERVICE_ID, DATE, TIME);
    }

    private Service classicService() {
        Service service = new Service();
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("8.00"));
        return service;
    }

    private void givenSlotIsOffered(boolean offered) {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(true);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(classicService()));
        when(availabilityService.getAvailableSlots(BARBER_ID, DATE, SERVICE_ID))
            .thenReturn(List.of(new TimeSlot(TIME, offered)));
    }

    private UserPrincipal principal(UUID id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@test.dev");
        user.setPasswordHash("hash");
        user.setFullName("Test " + role);
        user.setRole(role);
        return new UserPrincipal(user);
    }

    // ---- create ----

    @Test
    void create_cuandoElHorarioEstaOfrecido_creaLaCitaConfirmada() {
        givenSlotIsOffered(true);
        when(appointmentRepository.findActiveForCustomerInRange(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentResponse response = appointmentService().create(CUSTOMER_ID, request());

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(response.barberId()).isEqualTo(BARBER_ID);
        assertThat(response.priceAtBooking()).isEqualByComparingTo("8.00");
        verify(appointmentRepository).saveAndFlush(any(Appointment.class));
    }

    @Test
    void create_cuandoElBarberoNoExiste_lanzaResourceNotFound() {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> appointmentService().create(CUSTOMER_ID, request()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_cuandoElServicioNoExiste_lanzaResourceNotFound() {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(true);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService().create(CUSTOMER_ID, request()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_cuandoElHorarioNoEstaOfrecido_lanzaSlotNotAvailable() {
        givenSlotIsOffered(false);

        assertThatThrownBy(() -> appointmentService().create(CUSTOMER_ID, request()))
            .isInstanceOf(SlotNotAvailableException.class);
    }

    @Test
    void create_cuandoElClienteYaTieneOtraCitaAEsaHora_lanzaBusinessRule() {
        givenSlotIsOffered(true);
        Appointment otraCita = new Appointment();
        when(appointmentRepository.findActiveForCustomerInRange(any(), any(), any(), any())).thenReturn(List.of(otraCita));

        assertThatThrownBy(() -> appointmentService().create(CUSTOMER_ID, request()))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_cuandoLaBaseDeDatosRechazaPorSolapamiento_lanzaSlotNotAvailable() {
        // Esto simula la carrera real: el pre-chequeo de arriba pasó, pero
        // el EXCLUDE constraint de Postgres igual bloqueó el INSERT.
        givenSlotIsOffered(true);
        when(appointmentRepository.findActiveForCustomerInRange(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("constraint"));

        assertThatThrownBy(() -> appointmentService().create(CUSTOMER_ID, request()))
            .isInstanceOf(SlotNotAvailableException.class);
    }

    // ---- cancel / complete / no-show ----

    @Test
    void cancel_comoClienteDueno_permiteYCambiaEstado() {
        Appointment appointment = new Appointment();
        appointment.setCustomerId(CUSTOMER_ID);
        appointment.setBarberId(BARBER_ID);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService().cancel(id, principal(CUSTOMER_ID, Role.CUSTOMER));

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_comoClienteAjeno_lanzaAccessDenied() {
        Appointment appointment = new Appointment();
        appointment.setCustomerId(CUSTOMER_ID);
        appointment.setBarberId(BARBER_ID);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService().cancel(id, principal(UUID.randomUUID(), Role.CUSTOMER)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_unaCitaYaCompletada_lanzaBusinessRule() {
        Appointment appointment = new Appointment();
        appointment.setCustomerId(CUSTOMER_ID);
        appointment.setBarberId(BARBER_ID);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService().cancel(id, principal(CUSTOMER_ID, Role.CUSTOMER)))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void complete_comoBarberoDueno_permiteYCambiaEstado() {
        Appointment appointment = new Appointment();
        appointment.setCustomerId(CUSTOMER_ID);
        appointment.setBarberId(BARBER_ID);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService().complete(id, principal(BARBER_ID, Role.BARBER));

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void complete_comoElClienteDeLaCita_lanzaAccessDenied() {
        // Completar/no-show es del barbero, no del cliente, aunque sea SU cita.
        Appointment appointment = new Appointment();
        appointment.setCustomerId(CUSTOMER_ID);
        appointment.setBarberId(BARBER_ID);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService().complete(id, principal(CUSTOMER_ID, Role.CUSTOMER)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getForUser_comoAdmin_ignoraLaPropiedadYSiemprePermite() {
        Appointment appointment = new Appointment();
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setBarberId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = appointmentService().getForUser(id, principal(UUID.randomUUID(), Role.ADMIN));

        assertThat(response).isNotNull();
    }
}
