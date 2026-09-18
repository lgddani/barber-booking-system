package com.barberbooking.api.barbers;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursItem(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {
    static WorkingHoursItem from(WorkingHours wh) {
        return new WorkingHoursItem(wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime());
    }
}
