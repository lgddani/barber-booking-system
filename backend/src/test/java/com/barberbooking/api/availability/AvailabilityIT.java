package com.barberbooking.api.availability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barberbooking.api.appointments.Appointment;
import com.barberbooking.api.appointments.AppointmentStatus;
import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

// Contra datos reales sembrados por el propio test (no simulados con
// Mockito): sirve para confirmar que las piezas de verdad encajan entre sí.
class AvailabilityIT extends AbstractIntegrationTest {

    @Test
    void combinaJornadaLaboralYCitaExistenteParaCalcularLosHorarios() throws Exception {
        User admin = createUser("admin-avail@test.dev", Role.ADMIN);
        User barber = createBarber("barber-avail@test.dev");
        User customer = createUser("cliente-avail@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        Appointment existing = new Appointment();
        existing.setCustomerId(customer.getId());
        existing.setBarberId(barber.getId());
        existing.setServiceId(service.getId());
        existing.setStartAt(monday.atTime(9, 30).atZone(BUSINESS_ZONE).toInstant());
        existing.setEndAt(monday.atTime(10, 0).atZone(BUSINESS_ZONE).toInstant());
        existing.setStatus(AppointmentStatus.CONFIRMED);
        existing.setPriceAtBooking(service.getPrice());
        appointmentRepository.save(existing);

        mockMvc.perform(get("/api/v1/barbers/" + barber.getId() + "/availability")
                .param("date", monday.toString())
                .param("serviceId", service.getId().toString())
                .header("Authorization", bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.time=='09:00:00')].available").value(true))
            .andExpect(jsonPath("$[?(@.time=='09:30:00')].available").value(false))
            .andExpect(jsonPath("$[?(@.time=='10:00:00')].available").value(true));
    }

    @Test
    void barberoInexistente_devuelve404() throws Exception {
        User admin = createUser("admin-avail2@test.dev", Role.ADMIN);
        Service service = seedService();

        mockMvc.perform(get("/api/v1/barbers/" + java.util.UUID.randomUUID() + "/availability")
                .param("date", nextMonday().toString())
                .param("serviceId", service.getId().toString())
                .header("Authorization", bearer(admin)))
            .andExpect(status().isNotFound());
    }
}
