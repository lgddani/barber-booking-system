package com.barberbooking.api.barbers;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {

    List<WorkingHours> findByBarberIdOrderByDayOfWeekAscStartTimeAsc(UUID barberId);

    List<WorkingHours> findByBarberIdAndDayOfWeekOrderByStartTimeAsc(UUID barberId, DayOfWeek dayOfWeek);

    void deleteByBarberId(UUID barberId);
}
