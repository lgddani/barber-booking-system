package com.barberbooking.api.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

// La prueba estrella del proyecto: automatiza lo que en la Fase 5 se probó a
// mano con 12 `curl` en paralelo. Varios hilos disparan la misma petición al
// mismo tiempo de verdad (sincronizados con un CountDownLatch), y se
// verifica que la base de datos —no la aplicación— dejó pasar solo una.
class AppointmentConcurrencyIT extends AbstractIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 15;

    @Test
    void soloUnaReservaTieneExito_cuandoVariosClientesPidenElMismoHorarioALaVez() throws Exception {
        User barber = createBarber("barber-race@test.dev");
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        List<User> customers = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            customers.add(createUser("race" + i + "@test.dev", Role.CUSTOMER));
        }

        String body = objectMapper.writeValueAsString(
            new AppointmentCreateRequest(barber.getId(), service.getId(), monday, LocalTime.of(9, 0))
        );

        List<Integer> statusCodes = fireConcurrently(customers, body);

        long created = statusCodes.stream().filter(s -> s == 201).count();
        long conflicted = statusCodes.stream().filter(s -> s == 409).count();
        assertThat(created).isEqualTo(1);
        assertThat(conflicted).isEqualTo(CONCURRENT_REQUESTS - 1);

        long confirmedInDb = appointmentRepository.findByBarberIdOrderByStartAtAsc(barber.getId()).stream()
            .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
            .count();
        assertThat(confirmedInDb).isEqualTo(1);
    }

    @Test
    void variasReservasSimultaneasParaHorariosDistintos_todasTienenExito() throws Exception {
        User barber = createBarber("barber-race2@test.dev");
        Service service = seedService();
        LocalDate monday = nextMonday();
        seedMondayHours(barber.getId());

        List<LocalTime> times = List.of(
            LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0)
        );
        List<User> customers = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            customers.add(createUser("distinct" + i + "@test.dev", Role.CUSTOMER));
        }
        List<String> bodies = new ArrayList<>();
        for (LocalTime time : times) {
            bodies.add(objectMapper.writeValueAsString(
                new AppointmentCreateRequest(barber.getId(), service.getId(), monday, time)
            ));
        }

        List<Integer> statusCodes = fireConcurrentlyWithDistinctBodies(customers, bodies);

        assertThat(statusCodes).allMatch(status -> status == 201);
    }

    private List<Integer> fireConcurrently(List<User> customers, String sameBody) throws Exception {
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++) {
            bodies.add(sameBody);
        }
        return fireConcurrentlyWithDistinctBodies(customers, bodies);
    }

    // Todos los hilos se registran en readyLatch y esperan en startLatch: el
    // hilo principal no los suelta hasta que TODOS están listos, así salen
    // lo más simultáneo posible en vez de uno detrás de otro.
    private List<Integer> fireConcurrentlyWithDistinctBodies(List<User> customers, List<String> bodies) throws Exception {
        int n = customers.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch readyLatch = new CountDownLatch(n);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            User customer = customers.get(i);
            String body = bodies.get(i);
            futures.add(pool.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statusCodes.add(future.get(15, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return statusCodes;
    }
}
