package com.barberbooking.api.barbers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Override puntual para una fecha exacta: CLOSED cierra el día completo
// (ignorando la plantilla semanal), CUSTOM_HOURS la reemplaza con un rango
// distinto para ese día.
@Entity
@Table(name = "barber_schedule_exceptions")
@Getter
@Setter
@NoArgsConstructor
public class ScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "barber_id", nullable = false)
    private UUID barberId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionType type;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
}
