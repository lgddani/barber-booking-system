package com.barberbooking.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;
    private final Clock clock;

    // Clock inyectado (no Instant.now()/new Date() directo) para poder
    // probar tokens expirados en tests sin tener que esperar tiempo real.
    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes,
        Clock clock
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMinutes * 60_000;
        this.clock = clock;
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
            .subject(principal.getUsername())
            .claim("uid", principal.getId().toString())
            .claim("role", principal.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMillis)))
            .signWith(key)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername());
        } catch (ExpiredJwtException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        // .clock(...) usa el mismo reloj inyectado para decidir si "expiró";
        // sin esto, jjwt valida contra el reloj real del sistema operativo,
        // sin importar qué Clock le pasemos al resto de la clase — por eso
        // los tests con un Clock fijo en el pasado fallaban con
        // ExpiredJwtException en vez de comportarse como se esperaba.
        return Jwts.parser()
            .verifyWith(key)
            .clock(() -> Date.from(Instant.now(clock)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
