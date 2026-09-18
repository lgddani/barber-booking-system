package com.barberbooking.api.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.barberbooking.api.appointments.Appointment;
import com.barberbooking.api.appointments.AppointmentRepository;
import com.barberbooking.api.appointments.AppointmentStatus;
import com.barberbooking.api.barbers.ExceptionType;
import com.barberbooking.api.barbers.ScheduleException;
import com.barberbooking.api.barbers.ScheduleExceptionRepository;
import com.barberbooking.api.barbers.WorkingHours;
import com.barberbooking.api.barbers.WorkingHoursRepository;
import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.catalog.ServiceRepository;
import com.barberbooking.api.common.ResourceNotFoundException;
import com.barberbooking.api.users.BarberProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    // Lunes, para que coincida con la jornada laboral sembrada en el proyecto real.
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);
    private static final UUID BARBER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @Mock private WorkingHoursRepository workingHoursRepository;
    @Mock private ScheduleExceptionRepository scheduleExceptionRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BarberProfileRepository barberProfileRepository;

    private Service clasico30min;

    @BeforeEach
    void setUp() {
        clasico30min = new Service();
        clasico30min.setDurationMinutes(30);
    }

    // "Ahora" fijo, muy anterior a la fecha que se está consultando: así el
    // chequeo de "hora ya pasada" nunca interfiere salvo que el propio test
    // lo esté probando a propósito.
    private AvailabilityService service() {
        return service(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZONE));
    }

    private AvailabilityService service(Clock clock) {
        return new AvailabilityService(
            workingHoursRepository, scheduleExceptionRepository, appointmentRepository,
            serviceRepository, barberProfileRepository, clock, 30
        );
    }

    private WorkingHours hours(LocalTime start, LocalTime end) {
        WorkingHours wh = new WorkingHours();
        wh.setBarberId(BARBER_ID);
        wh.setDayOfWeek(DayOfWeek.MONDAY);
        wh.setStartTime(start);
        wh.setEndTime(end);
        return wh;
    }

    private void givenBarberAndServiceExist() {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(true);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(clasico30min));
    }

    @Test
    void diaNormal_generaSlotsCada30MinutosDentroDelHorario() {
        givenBarberAndServiceExist();
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of());
        when(workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(BARBER_ID, DayOfWeek.MONDAY))
            .thenReturn(List.of(hours(LocalTime.of(9, 0), LocalTime.of(13, 0))));
        when(appointmentRepository.findActiveInRange(eq(BARBER_ID), any(), any(), any())).thenReturn(List.of());

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        assertThat(slots).extracting(TimeSlot::time).containsExactly(
            LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30),
            LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(12, 0), LocalTime.of(12, 30)
        );
        assertThat(slots).allMatch(TimeSlot::available);
    }

    @Test
    void diaCerradoPorExcepcion_devuelveListaVacia() {
        givenBarberAndServiceExist();
        ScheduleException closed = new ScheduleException();
        closed.setType(ExceptionType.CLOSED);
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of(closed));

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        assertThat(slots).isEmpty();
    }

    @Test
    void excepcionConHorarioPersonalizado_reemplazaElHorarioSemanal() {
        givenBarberAndServiceExist();
        ScheduleException customHours = new ScheduleException();
        customHours.setType(ExceptionType.CUSTOM_HOURS);
        customHours.setStartTime(LocalTime.of(15, 0));
        customHours.setEndTime(LocalTime.of(16, 0));
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of(customHours));
        when(appointmentRepository.findActiveInRange(eq(BARBER_ID), any(), any(), any())).thenReturn(List.of());

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        // Nótese: NUNCA se consultó workingHoursRepository — la excepción manda.
        assertThat(slots).extracting(TimeSlot::time).containsExactly(LocalTime.of(15, 0), LocalTime.of(15, 30));
    }

    @Test
    void citaExistente_marcaEseHorarioComoNoDisponible() {
        givenBarberAndServiceExist();
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of());
        when(workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(BARBER_ID, DayOfWeek.MONDAY))
            .thenReturn(List.of(hours(LocalTime.of(9, 0), LocalTime.of(11, 0))));

        Appointment existing = new Appointment();
        existing.setStartAt(MONDAY.atTime(9, 30).atZone(ZONE).toInstant());
        existing.setEndAt(MONDAY.atTime(10, 0).atZone(ZONE).toInstant());
        when(appointmentRepository.findActiveInRange(eq(BARBER_ID), any(), any(), any())).thenReturn(List.of(existing));

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        assertThat(slots).filteredOn(s -> s.time().equals(LocalTime.of(9, 30))).extracting(TimeSlot::available).containsExactly(false);
        assertThat(slots).filteredOn(s -> !s.time().equals(LocalTime.of(9, 30))).allMatch(TimeSlot::available);
    }

    @Test
    void servicioLargo_noOfreceCandidatoQueNoCabeAntesDelCierre() {
        givenBarberAndServiceExist();
        clasico30min.setDurationMinutes(45);
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of());
        when(workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(BARBER_ID, DayOfWeek.MONDAY))
            .thenReturn(List.of(hours(LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(appointmentRepository.findActiveInRange(eq(BARBER_ID), any(), any(), any())).thenReturn(List.of());

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        // 09:00+45=09:45 cabe (<=10:00). 09:30+45=10:15 NO cabe: no debe aparecer.
        assertThat(slots).extracting(TimeSlot::time).containsExactly(LocalTime.of(9, 0));
    }

    @Test
    void horaYaPasadaHoy_seMarcaComoNoDisponible() {
        givenBarberAndServiceExist();
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of());
        when(workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(BARBER_ID, DayOfWeek.MONDAY))
            .thenReturn(List.of(hours(LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(appointmentRepository.findActiveInRange(eq(BARBER_ID), any(), any(), any())).thenReturn(List.of());

        // "Ahora" es ese mismo lunes a las 10:15.
        Clock todayAt1015 = Clock.fixed(MONDAY.atTime(10, 15).atZone(ZONE).toInstant(), ZONE);

        List<TimeSlot> slots = service(todayAt1015).getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        assertThat(slots).filteredOn(s -> s.time().isBefore(LocalTime.of(10, 15))).allMatch(s -> !s.available());
        assertThat(slots).filteredOn(s -> !s.time().isBefore(LocalTime.of(10, 15))).allMatch(TimeSlot::available);
    }

    @Test
    void barberoSinHorarioEseDia_devuelveListaVacia() {
        givenBarberAndServiceExist();
        when(scheduleExceptionRepository.findByBarberIdAndDate(BARBER_ID, MONDAY)).thenReturn(List.of());
        when(workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(BARBER_ID, DayOfWeek.MONDAY))
            .thenReturn(List.of());

        List<TimeSlot> slots = service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID);

        assertThat(slots).isEmpty();
    }

    @Test
    void barberoInexistente_lanzaResourceNotFound() {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void servicioInexistente_lanzaResourceNotFound() {
        when(barberProfileRepository.existsById(BARBER_ID)).thenReturn(true);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getAvailableSlots(BARBER_ID, MONDAY, SERVICE_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
