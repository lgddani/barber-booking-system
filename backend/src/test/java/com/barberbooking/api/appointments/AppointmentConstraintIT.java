package com.barberbooking.api.appointments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

// La prueba de "defensa en profundidad": nos saltamos AppointmentService a
// propósito y vamos directo al repositorio, para confirmar que es la base de
// datos —no la lógica de la aplicación— la que realmente impide el
// solapamiento. Si algún día alguien reescribe el service con un bug, este
// test seguiría en verde/rojo según corresponda, sin depender de ese código.
class AppointmentConstraintIT extends AbstractIntegrationTest {

    @Test
    void elExcludeConstraint_bloqueaElSolapamientoAunSinPasarPorElServiceLayer() {
        User barber = createBarber("barber-constraint@test.dev");
        User customer1 = createUser("cliente1-constraint@test.dev", Role.CUSTOMER);
        User customer2 = createUser("cliente2-constraint@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();

        Appointment first = new Appointment();
        first.setCustomerId(customer1.getId());
        first.setBarberId(barber.getId());
        first.setServiceId(service.getId());
        first.setStartAt(monday.atTime(9, 0).atZone(BUSINESS_ZONE).toInstant());
        first.setEndAt(monday.atTime(9, 30).atZone(BUSINESS_ZONE).toInstant());
        first.setStatus(AppointmentStatus.CONFIRMED);
        first.setPriceAtBooking(service.getPrice());
        appointmentRepository.saveAndFlush(first);

        Appointment overlapping = new Appointment();
        overlapping.setCustomerId(customer2.getId());
        overlapping.setBarberId(barber.getId()); // mismo barbero
        overlapping.setServiceId(service.getId());
        overlapping.setStartAt(monday.atTime(9, 15).atZone(BUSINESS_ZONE).toInstant()); // se solapa 15 min
        overlapping.setEndAt(monday.atTime(9, 45).atZone(BUSINESS_ZONE).toInstant());
        overlapping.setStatus(AppointmentStatus.CONFIRMED);
        overlapping.setPriceAtBooking(service.getPrice());

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(overlapping))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unaCitaCanceladaNoCuentaParaElSolapamiento() {
        // El EXCLUDE constraint tiene WHERE status NOT IN (CANCELLED, NO_SHOW):
        // una cita cancelada debe poder coexistir con otra en el mismo horario.
        User barber = createBarber("barber-constraint2@test.dev");
        User customer1 = createUser("cliente1-constraint2@test.dev", Role.CUSTOMER);
        User customer2 = createUser("cliente2-constraint2@test.dev", Role.CUSTOMER);
        Service service = seedService();
        LocalDate monday = nextMonday();

        Appointment cancelled = new Appointment();
        cancelled.setCustomerId(customer1.getId());
        cancelled.setBarberId(barber.getId());
        cancelled.setServiceId(service.getId());
        cancelled.setStartAt(monday.atTime(9, 0).atZone(BUSINESS_ZONE).toInstant());
        cancelled.setEndAt(monday.atTime(9, 30).atZone(BUSINESS_ZONE).toInstant());
        cancelled.setStatus(AppointmentStatus.CANCELLED);
        cancelled.setPriceAtBooking(service.getPrice());
        appointmentRepository.saveAndFlush(cancelled);

        Appointment sameSlot = new Appointment();
        sameSlot.setCustomerId(customer2.getId());
        sameSlot.setBarberId(barber.getId());
        sameSlot.setServiceId(service.getId());
        sameSlot.setStartAt(monday.atTime(9, 0).atZone(BUSINESS_ZONE).toInstant());
        sameSlot.setEndAt(monday.atTime(9, 30).atZone(BUSINESS_ZONE).toInstant());
        sameSlot.setStatus(AppointmentStatus.CONFIRMED);
        sameSlot.setPriceAtBooking(service.getPrice());

        // No debe lanzar: la cancelada no cuenta.
        appointmentRepository.saveAndFlush(sameSlot);
    }
}
