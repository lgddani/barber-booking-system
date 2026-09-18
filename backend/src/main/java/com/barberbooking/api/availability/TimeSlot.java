package com.barberbooking.api.availability;

import java.time.LocalTime;

public record TimeSlot(LocalTime time, boolean available) {
}
