package com.barberbooking.api.appointments;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AppointmentLifecycleIT extends AbstractIntegrationTest {

    @Test
    void crear_completar_yLuegoCancelarSeRechaza() throws Exception {
        User barber = createBarber("barber-lc@test.dev");
        User customer = createUser("cliente-lc@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        String createBody = objectMapper.writeValueAsString(
            new AppointmentCreateRequest(barber.getId(), service.getId(), monday, LocalTime.of(9, 0))
        );

        String response = mockMvc.perform(post("/api/v1/appointments")
                .header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/complete").header("Authorization", bearer(barber)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Ya está COMPLETED: cancelarla ahora debe rechazarse.
        mockMvc.perform(patch("/api/v1/appointments/" + id + "/cancel").header("Authorization", bearer(customer)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void crearYCancelar_elClienteDuenoPuedeCancelarLaSuya() throws Exception {
        User barber = createBarber("barber-lc2@test.dev");
        User customer = createUser("cliente-lc2@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        String createBody = objectMapper.writeValueAsString(
            new AppointmentCreateRequest(barber.getId(), service.getId(), monday, LocalTime.of(9, 0))
        );
        String response = mockMvc.perform(post("/api/v1/appointments")
                .header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/cancel").header("Authorization", bearer(customer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void reservarElMismoHorarioDosVeces_laSegundaFalla() throws Exception {
        User barber = createBarber("barber-lc3@test.dev");
        User customer1 = createUser("cliente-lc3a@test.dev", Role.CUSTOMER);
        User customer2 = createUser("cliente-lc3b@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        String body = objectMapper.writeValueAsString(
            new AppointmentCreateRequest(barber.getId(), service.getId(), monday, LocalTime.of(9, 0))
        );

        mockMvc.perform(post("/api/v1/appointments")
                .header("Authorization", bearer(customer1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments")
                .header("Authorization", bearer(customer2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }
}
