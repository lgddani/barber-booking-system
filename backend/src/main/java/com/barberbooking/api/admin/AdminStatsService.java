package com.barberbooking.api.admin;

import com.barberbooking.api.appointments.AppointmentRepository;
import com.barberbooking.api.appointments.AppointmentStatus;
import com.barberbooking.api.appointments.BarberCount;
import com.barberbooking.api.appointments.StatusCount;
import com.barberbooking.api.common.BusinessRuleException;
import com.barberbooking.api.users.User;
import com.barberbooking.api.users.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AdminStatsResponse getStats(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveFrom = from != null ? from : today.minusMonths(1);
        LocalDate effectiveTo = to != null ? to : today;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BusinessRuleException("'from' no puede ser posterior a 'to'");
        }

        ZoneId zone = clock.getZone();
        Instant fromInstant = effectiveFrom.atStartOfDay(zone).toInstant();
        Instant toInstant = effectiveTo.plusDays(1).atStartOfDay(zone).toInstant();

        List<StatusCount> statusCounts = appointmentRepository.countByStatusGroupedInRange(fromInstant, toInstant);
        var byStatus = statusCounts.stream()
            .collect(Collectors.toMap(StatusCount::status, StatusCount::count));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        var revenue = appointmentRepository.sumPriceByStatusInRange(fromInstant, toInstant, AppointmentStatus.COMPLETED);

        List<BarberCount> barberCounts = appointmentRepository.countByBarberInRange(fromInstant, toInstant);
        AdminStatsResponse.BusiestBarber busiest = barberCounts.stream()
            .findFirst()
            .map(bc -> new AdminStatsResponse.BusiestBarber(
                bc.barberId(),
                userRepository.findById(bc.barberId()).map(User::getFullName).orElse("Desconocido"),
                bc.count()
            ))
            .orElse(null);

        return new AdminStatsResponse(effectiveFrom, effectiveTo, total, byStatus, revenue, busiest);
    }
}
