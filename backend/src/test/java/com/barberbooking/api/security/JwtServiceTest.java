package com.barberbooking.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-characters-long";
    private static final ZoneId ZONE = ZoneOffset.UTC;

    private UserPrincipal principal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("carlos@barberbooking.dev");
        user.setPasswordHash("hash");
        user.setFullName("Carlos Ruiz");
        user.setRole(Role.BARBER);
        return new UserPrincipal(user);
    }

    @Test
    void generateToken_thenExtractUsername_returnsSameEmail() {
        JwtService jwtService = new JwtService(SECRET, 60, Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZONE));
        UserPrincipal principal = principal();

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.extractUsername(token)).isEqualTo(principal.getUsername());
    }

    @Test
    void isValid_forFreshToken_returnsTrue() {
        Clock now = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZONE);
        JwtService jwtService = new JwtService(SECRET, 60, now);
        UserPrincipal principal = principal();

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isValid(token, principal)).isTrue();
    }

    @Test
    void isValid_whenTokenExpired_returnsFalse() {
        // Se genera el token con el reloj en las 10:00 y expiración de 1 minuto...
        Clock whenIssued = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZONE);
        JwtService issuer = new JwtService(SECRET, 1, whenIssued);
        UserPrincipal principal = principal();
        String token = issuer.generateToken(principal);

        // ...pero se valida con el reloj adelantado a las 10:05: ya venció,
        // sin tener que esperar 5 minutos reales en el test.
        Clock fiveMinutesLater = Clock.fixed(Instant.parse("2026-01-01T10:05:00Z"), ZONE);
        JwtService validator = new JwtService(SECRET, 1, fiveMinutesLater);

        assertThat(validator.isValid(token, principal)).isFalse();
    }

    @Test
    void isValid_whenUsernameDoesNotMatch_returnsFalse() {
        Clock now = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZONE);
        JwtService jwtService = new JwtService(SECRET, 60, now);
        String token = jwtService.generateToken(principal());

        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("otro@barberbooking.dev");
        otherUser.setPasswordHash("hash");
        otherUser.setFullName("Otro");
        otherUser.setRole(Role.CUSTOMER);

        assertThat(jwtService.isValid(token, new UserPrincipal(otherUser))).isFalse();
    }
}
