package com.barberbooking.api.appointments;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // Trae las citas del barbero cuyo rango [startAt, endAt) se solapa con
    // [from, to) — se usa para saber qué horarios ya están ocupados ese día.
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.barberId = :barberId
          AND a.status NOT IN :excludedStatuses
          AND a.startAt < :to
          AND a.endAt > :from
        """)
    List<Appointment> findActiveInRange(
        @Param("barberId") UUID barberId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses
    );
}
