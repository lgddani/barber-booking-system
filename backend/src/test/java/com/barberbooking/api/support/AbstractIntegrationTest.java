package com.barberbooking.api.support;

import com.barberbooking.api.TestcontainersConfiguration;
import com.barberbooking.api.appointments.AppointmentRepository;
import com.barberbooking.api.barbers.ScheduleExceptionRepository;
import com.barberbooking.api.barbers.WorkingHours;
import com.barberbooking.api.barbers.WorkingHoursRepository;
import com.barberbooking.api.catalog.Service;
import com.barberbooking.api.catalog.ServiceRepository;
import com.barberbooking.api.security.JwtService;
import com.barberbooking.api.security.UserPrincipal;
import com.barberbooking.api.users.BarberProfile;
import com.barberbooking.api.users.BarberProfileRepository;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import com.barberbooking.api.users.UserRepository;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Base común para todos los tests de integración: levanta Spring completo
// contra un Postgres real en Docker (Testcontainers), no una base falsa.
// @ActiveProfiles("test") a propósito NO activa el perfil "dev": así el
// DevDataSeeder no siembra nada y cada test controla sus propios datos.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BarberProfileRepository barberProfileRepository;

    @Autowired
    protected ServiceRepository serviceRepository;

    @Autowired
    protected WorkingHoursRepository workingHoursRepository;

    @Autowired
    protected ScheduleExceptionRepository scheduleExceptionRepository;

    @Autowired
    protected AppointmentRepository appointmentRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName("Test " + role);
        user.setRole(role);
        return userRepository.save(user);
    }

    protected User createBarber(String email) {
        User user = createUser(email, Role.BARBER);
        BarberProfile profile = new BarberProfile();
        profile.setUser(user);
        profile.setBio("Barbero de prueba");
        barberProfileRepository.save(profile);
        return user;
    }

    protected String tokenFor(User user) {
        return jwtService.generateToken(new UserPrincipal(user));
    }

    protected String bearer(User user) {
        return "Bearer " + tokenFor(user);
    }

    protected static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Guayaquil");

    // Un lunes futuro real, para que las pruebas nunca choquen entre sí ni
    // caigan por accidente en "hora ya pasada".
    protected LocalDate nextMonday() {
        LocalDate date = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    protected Service seedService() {
        Service service = new Service();
        service.setName("Corte de prueba");
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("8.00"));
        service.setActive(true);
        return serviceRepository.save(service);
    }

    protected void seedMondayHours(java.util.UUID barberId) {
        WorkingHours hours = new WorkingHours();
        hours.setBarberId(barberId);
        hours.setDayOfWeek(DayOfWeek.MONDAY);
        hours.setStartTime(LocalTime.of(9, 0));
        hours.setEndTime(LocalTime.of(13, 0));
        workingHoursRepository.save(hours);
    }

    // Se corre después de CADA test (no solo por clase) para que ningún test
    // dependa del orden de ejecución ni vea datos de otro.
    @AfterEach
    void cleanupTestData() {
        appointmentRepository.deleteAll();
        scheduleExceptionRepository.deleteAll();
        workingHoursRepository.deleteAll();
        barberProfileRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();
    }
}
