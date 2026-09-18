package com.barberbooking.api.barbers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionResponse(UUID id, LocalDate date, ExceptionType type, LocalTime startTime, LocalTime endTime) {
    static ScheduleExceptionResponse from(ScheduleException ex) {
        return new ScheduleExceptionResponse(ex.getId(), ex.getDate(), ex.getType(), ex.getStartTime(), ex.getEndTime());
    }
}
