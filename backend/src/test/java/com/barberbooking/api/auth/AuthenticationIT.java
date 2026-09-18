package com.barberbooking.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthenticationIT extends AbstractIntegrationTest {

    @Test
    void registroYLogin_devuelvenUnTokenQueAccedeAUnEndpointProtegido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nuevo-auth@test.dev","password":"password123","fullName":"Nuevo Usuario"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CUSTOMER"));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nuevo-auth@test.dev","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("nuevo-auth@test.dev"));
    }

    @Test
    void loginConContrasenaIncorrecta_recibe401() throws Exception {
        createUser("existente-auth@test.dev", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"existente-auth@test.dev","password":"contrasena-incorrecta"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_recibe401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenInvalido_recibe401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer esto-no-es-un-token-valido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void conRolIncorrecto_recibe403() throws Exception {
        User customer = createUser("cliente-auth@test.dev", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/services")
                .header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Hackeo","durationMinutes":10,"price":1}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void registrarsePidiendoRolAdmin_igualCreaUnCustomer() throws Exception {
        // Test de regresión de seguridad: aunque el body pida ser ADMIN, el
        // registro público siempre debe forzar CUSTOMER.
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"hacker-auth@test.dev","password":"password123","fullName":"Hacker","role":"ADMIN"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}
