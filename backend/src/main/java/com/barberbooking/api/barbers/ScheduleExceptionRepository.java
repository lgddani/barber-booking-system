package com.barberbooking.api.barbers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {

    List<ScheduleException> findByBarberIdOrderByDateAsc(UUID barberId);

    List<ScheduleException> findByBarberIdAndDateBetweenOrderByDateAsc(UUID barberId, LocalDate from, LocalDate to);

    List<ScheduleException> findByBarberIdAndDate(UUID barberId, LocalDate date);
}
