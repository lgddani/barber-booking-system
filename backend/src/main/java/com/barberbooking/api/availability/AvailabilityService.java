package com.barberbooking.api.availability;

import com.barberbooking.api.appointments.AppointmentRepository;
import com.barberbooking.api.appointments.AppointmentStatus;
import com.barberbooking.api.barbers.ExceptionType;
import com.barberbooking.api.barbers.ScheduleException;
import com.barberbooking.api.barbers.ScheduleExceptionRepository;
import com.barberbooking.api.barbers.WorkingHours;
import com.barberbooking.api.barbers.WorkingHoursRepository;
import com.barberbooking.api.catalog.ServiceRepository;
import com.barberbooking.api.common.ResourceNotFoundException;
import com.barberbooking.api.users.BarberProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    private final WorkingHoursRepository workingHoursRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final Clock clock;
    private final int slotGranularityMinutes;

    // Constructor explícito (no Lombok) para que slotGranularityMinutes se
    // pueda pasar directo en los tests unitarios, sin depender de que Spring
    // esté levantado para resolver el @Value.
    public AvailabilityService(
        WorkingHoursRepository workingHoursRepository,
        ScheduleExceptionRepository scheduleExceptionRepository,
        AppointmentRepository appointmentRepository,
        ServiceRepository serviceRepository,
        BarberProfileRepository barberProfileRepository,
        Clock clock,
        @Value("${app.booking.slot-granularity-minutes:30}") int slotGranularityMinutes
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.barberProfileRepository = barberProfileRepository;
        this.clock = clock;
        this.slotGranularityMinutes = slotGranularityMinutes;
    }

    public List<TimeSlot> getAvailableSlots(UUID barberId, LocalDate date, UUID serviceId) {
        if (!barberProfileRepository.existsById(barberId)) {
            throw new ResourceNotFoundException("Barbero no encontrado: " + barberId);
        }
        int durationMinutes = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + serviceId))
            .getDurationMinutes();

        List<LocalTimeRange> workingRanges = resolveWorkingRanges(barberId, date);
        List<LocalTimeRange> busyRanges = resolveBusyRanges(barberId, date);
        LocalTime nowIfToday = date.equals(LocalDate.now(clock)) ? LocalTime.now(clock) : null;

        List<TimeSlot> slots = new ArrayList<>();
        for (LocalTimeRange range : workingRanges) {
            LocalTime candidateStart = range.start();
            while (true) {
                LocalTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
                // Se pasó de medianoche (plusMinutes da la vuelta) o ya no
                // cabe completo antes del cierre: no hay más candidatos en
                // este rango.
                if (!candidateEnd.isAfter(candidateStart) || candidateEnd.isAfter(range.end())) {
                    break;
                }
                LocalTime slotStart = candidateStart;
                LocalTime slotEnd = candidateEnd;
                boolean overlapsBusy = busyRanges.stream().anyMatch(busy -> busy.overlaps(slotStart, slotEnd));
                boolean isPast = nowIfToday != null && slotStart.isBefore(nowIfToday);
                slots.add(new TimeSlot(slotStart, !overlapsBusy && !isPast));
                candidateStart = candidateStart.plusMinutes(slotGranularityMinutes);
            }
        }

        slots.sort(Comparator.comparing(TimeSlot::time));
        return slots;
    }

    private List<LocalTimeRange> resolveWorkingRanges(UUID barberId, LocalDate date) {
        List<ScheduleException> exceptions = scheduleExceptionRepository.findByBarberIdAndDate(barberId, date);

        boolean closed = exceptions.stream().anyMatch(e -> e.getType() == ExceptionType.CLOSED);
        if (closed) {
            return List.of();
        }

        List<ScheduleException> customHours = exceptions.stream()
            .filter(e -> e.getType() == ExceptionType.CUSTOM_HOURS)
            .toList();
        if (!customHours.isEmpty()) {
            return customHours.stream()
                .map(e -> new LocalTimeRange(e.getStartTime(), e.getEndTime()))
                .toList();
        }

        return workingHoursRepository.findByBarberIdAndDayOfWeekOrderByStartTimeAsc(barberId, date.getDayOfWeek()).stream()
            .map(wh -> new LocalTimeRange(wh.getStartTime(), wh.getEndTime()))
            .toList();
    }

    private List<LocalTimeRange> resolveBusyRanges(UUID barberId, LocalDate date) {
        ZoneId zone = clock.getZone();
        Instant startOfDay = date.atStartOfDay(zone).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);

        return appointmentRepository.findActiveInRange(barberId, startOfDay, endOfDay, excluded).stream()
            .map(a -> new LocalTimeRange(
                LocalTime.from(a.getStartAt().atZone(zone)),
                LocalTime.from(a.getEndAt().atZone(zone))
            ))
            .toList();
    }

    private record LocalTimeRange(LocalTime start, LocalTime end) {
        boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
            return start.isBefore(otherEnd) && otherStart.isBefore(end);
        }
    }
}
