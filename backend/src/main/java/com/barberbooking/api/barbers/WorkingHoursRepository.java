package com.barberbooking.api.barbers;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {
}
