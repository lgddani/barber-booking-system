package com.barberbooking.api.appointments;

import java.math.BigDecimal;
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

    // Misma idea pero por cliente: para la regla "no puedes tener dos citas
    // simultáneas", sin importar con qué barbero.
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.customerId = :customerId
          AND a.status NOT IN :excludedStatuses
          AND a.startAt < :to
          AND a.endAt > :from
        """)
    List<Appointment> findActiveForCustomerInRange(
        @Param("customerId") UUID customerId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("excludedStatuses") Collection<AppointmentStatus> excludedStatuses
    );

    List<Appointment> findByCustomerIdOrderByStartAtAsc(UUID customerId);

    List<Appointment> findByCustomerIdAndStatusOrderByStartAtAsc(UUID customerId, AppointmentStatus status);

    List<Appointment> findByBarberIdOrderByStartAtAsc(UUID barberId);

    List<Appointment> findByBarberIdAndStatusOrderByStartAtAsc(UUID barberId, AppointmentStatus status);

    List<Appointment> findByStatusOrderByStartAtAsc(AppointmentStatus status);

    List<Appointment> findAllByOrderByStartAtAsc();

    @Query("""
        SELECT new com.barberbooking.api.appointments.StatusCount(a.status, COUNT(a))
        FROM Appointment a
        WHERE a.startAt >= :from AND a.startAt < :to
        GROUP BY a.status
        """)
    List<StatusCount> countByStatusGroupedInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(a.priceAtBooking), 0) FROM Appointment a
        WHERE a.startAt >= :from AND a.startAt < :to AND a.status = :status
        """)
    BigDecimal sumPriceByStatusInRange(
        @Param("from") Instant from, @Param("to") Instant to, @Param("status") AppointmentStatus status
    );

    @Query("""
        SELECT new com.barberbooking.api.appointments.BarberCount(a.barberId, COUNT(a))
        FROM Appointment a
        WHERE a.startAt >= :from AND a.startAt < :to
        GROUP BY a.barberId
        ORDER BY COUNT(a) DESC
        """)
    List<BarberCount> countByBarberInRange(@Param("from") Instant from, @Param("to") Instant to);
}
