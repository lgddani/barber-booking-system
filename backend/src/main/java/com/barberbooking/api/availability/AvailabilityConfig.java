package com.barberbooking.api.availability;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvailabilityConfig {

    // Un Clock inyectable (en vez de LocalDate.now()/Instant.now() directo en
    // el servicio) para que "ahora" se pueda fijar de forma determinística
    // cuando lleguemos a los tests.
    @Bean
    public Clock clock(@Value("${app.business-timezone:America/Bogota}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
