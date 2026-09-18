package com.barberbooking.api.barbers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {

    List<WorkingHours> findByBarberIdOrderByDayOfWeekAscStartTimeAsc(UUID barberId);

    void deleteByBarberId(UUID barberId);
}
