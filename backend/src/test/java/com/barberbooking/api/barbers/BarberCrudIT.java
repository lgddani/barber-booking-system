package com.barberbooking.api.barbers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barberbooking.api.support.AbstractIntegrationTest;
import com.barberbooking.api.users.Role;
import com.barberbooking.api.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BarberCrudIT extends AbstractIntegrationTest {

    @Test
    void admin_puedeCrearListarActualizarDesactivarYReactivarUnBarbero() throws Exception {
        User admin = createUser("admin-crud@test.dev", Role.ADMIN);
        String auth = bearer(admin);

        String response = mockMvc.perform(post("/api/v1/barbers")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nuevo-barbero@test.dev","password":"password123","fullName":"Nuevo Barbero","bio":"Recien contratado"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fullName").value("Nuevo Barbero"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/barbers").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        mockMvc.perform(put("/api/v1/barbers/" + id)
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Nuevo Barbero Actualizado","bio":"Bio actualizada"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Nuevo Barbero Actualizado"));

        mockMvc.perform(delete("/api/v1/barbers/" + id).header("Authorization", auth))
            .andExpect(status().isNoContent());

        // Desactivado: no sale en el listado normal...
        mockMvc.perform(get("/api/v1/barbers").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());

        // ...pero sigue existiendo si se piden también los inactivos.
        mockMvc.perform(get("/api/v1/barbers").param("includeInactive", "true").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        // Reactivar: vuelve a aparecer en el listado normal.
        mockMvc.perform(patch("/api/v1/barbers/" + id + "/activate").header("Authorization", auth))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/barbers").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void unCliente_noPuedeCrearBarberosNiListarInactivos() throws Exception {
        User cliente = createUser("cliente-crud@test.dev", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/barbers")
                .header("Authorization", bearer(cliente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"hackeo@test.dev","password":"password123","fullName":"Hackeo"}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/barbers").param("includeInactive", "true").header("Authorization", bearer(cliente)))
            .andExpect(status().isForbidden());
    }

    @Test
    void unBarbero_puedeActualizarSuPropioPerfilPeroNoElDeOtro() throws Exception {
        User propio = createBarber("propio@test.dev");
        User otro = createBarber("otro@test.dev");

        mockMvc.perform(put("/api/v1/barbers/" + propio.getId())
                .header("Authorization", bearer(propio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Mi Nombre Actualizado","bio":"Mi bio"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Mi Nombre Actualizado"));

        mockMvc.perform(put("/api/v1/barbers/" + otro.getId())
                .header("Authorization", bearer(propio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Intento ajeno","bio":"no deberia pasar"}
                    """))
            .andExpect(status().isForbidden());
    }
}
