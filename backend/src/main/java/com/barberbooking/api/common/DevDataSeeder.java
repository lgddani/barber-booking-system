package com.barberbooking.api.common;

import com.barberbooking.api.barbers.WorkingHours;
import com.barberbooking.api.barbers.WorkingHoursRepository;
import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.catalog.ServiceRepository;
import com.barberbooking.api.users.BarberProfile;
import com.barberbooking.api.users.BarberProfileRepository;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import com.barberbooking.api.users.UserRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Puebla la base con datos de ejemplo (admin, barberos, clientes, servicios)
// para que el proyecto se vea listo para usar apenas se levanta. Vive en esta
// fase porque necesita el PasswordEncoder, que recién existe con la seguridad
// configurada. Solo corre con el perfil "dev" y solo si la base está vacía.
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private static final List<DayOfWeek> DIAS_LABORALES = List.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private final UserRepository userRepository;
    private final BarberProfileRepository barberProfileRepository;
    private final ServiceRepository serviceRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Sembrando datos de demo...");

        userRepository.save(newUser("admin@barberbooking.dev", "admin123", "Admin Demo", Role.ADMIN));

        User carlos = newUser("carlos@barberbooking.dev", "barbero123", "Carlos Ruiz", Role.BARBER);
        User ana = newUser("ana@barberbooking.dev", "barbero123", "Ana Torres", Role.BARBER);
        userRepository.saveAll(List.of(carlos, ana));
        seedBarberProfile(carlos, "Especialista en cortes clásicos y degradados.");
        seedBarberProfile(ana, "Especialista en barba y afeitado clásico.");
        seedWeeklyHours(carlos.getId());
        seedWeeklyHours(ana.getId());

        userRepository.save(newUser("cliente1@barberbooking.dev", "cliente123", "Juan Pérez", Role.CUSTOMER));
        userRepository.save(newUser("cliente2@barberbooking.dev", "cliente123", "María Gómez", Role.CUSTOMER));

        seedService("Corte clásico", 30, "8.00");
        seedService("Corte + barba", 45, "12.00");
        seedService("Barba", 20, "6.00");
        seedService("Corte + lavado", 60, "15.00");

        log.info("Datos de demo listos. Admin: admin@barberbooking.dev / admin123");
    }

    private User newUser(String email, String rawPassword, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        return user;
    }

    private void seedBarberProfile(User barber, String bio) {
        BarberProfile profile = new BarberProfile();
        profile.setUser(barber);
        profile.setBio(bio);
        barberProfileRepository.save(profile);
    }

    private void seedWeeklyHours(UUID barberId) {
        for (DayOfWeek day : DIAS_LABORALES) {
            workingHoursRepository.save(hours(barberId, day, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            workingHoursRepository.save(hours(barberId, day, LocalTime.of(14, 0), LocalTime.of(18, 0)));
        }
    }

    private WorkingHours hours(UUID barberId, DayOfWeek day, LocalTime start, LocalTime end) {
        WorkingHours wh = new WorkingHours();
        wh.setBarberId(barberId);
        wh.setDayOfWeek(day);
        wh.setStartTime(start);
        wh.setEndTime(end);
        return wh;
    }

    private void seedService(String name, int durationMinutes, String price) {
        Service service = new Service();
        service.setName(name);
        service.setDurationMinutes(durationMinutes);
        service.setPrice(new BigDecimal(price));
        serviceRepository.save(service);
    }
}
