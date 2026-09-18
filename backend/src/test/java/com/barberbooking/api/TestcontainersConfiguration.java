package com.barberbooking.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// public (no package-private, como lo dejó el generador) para que cualquier
// test de integración, en cualquier paquete, lo pueda importar.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Misma versión que docker-compose.yml, no "latest" — así el Postgres de
    // los tests es el mismo que el de desarrollo/producción, no uno distinto
    // que cambia solo cuando se re-descarga la imagen.
    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
