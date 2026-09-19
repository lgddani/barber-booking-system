package com.barberbooking.api.catalog;

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

class ServiceCrudIT extends AbstractIntegrationTest {

    @Test
    void admin_puedeCrearListarActualizarYDesactivarUnServicio() throws Exception {
        User admin = createUser("admin-crud@test.dev", Role.ADMIN);
        String auth = bearer(admin);

        String response = mockMvc.perform(post("/api/v1/services")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Tinte","description":"Tinte de cabello","durationMinutes":40,"price":10.00}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Tinte"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/services").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        mockMvc.perform(put("/api/v1/services/" + id)
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Tinte premium","durationMinutes":40,"price":12.00}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Tinte premium"));

        mockMvc.perform(delete("/api/v1/services/" + id).header("Authorization", auth))
            .andExpect(status().isNoContent());

        // Desactivado (soft delete): no sale en el listado normal...
        mockMvc.perform(get("/api/v1/services").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").doesNotExist());

        // ...pero la fila sigue existiendo si se piden también los inactivos.
        mockMvc.perform(get("/api/v1/services").param("includeInactive", "true").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        // Reactivar: vuelve a aparecer en el listado normal.
        mockMvc.perform(patch("/api/v1/services/" + id + "/activate").header("Authorization", auth))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/services").header("Authorization", auth))
            .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void unCliente_noPuedeCrearServicios() throws Exception {
        User cliente = createUser("cliente-crud@test.dev", Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/services")
                .header("Authorization", bearer(cliente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Hackeo","durationMinutes":10,"price":1}
                    """))
            .andExpect(status().isForbidden());
    }
}
