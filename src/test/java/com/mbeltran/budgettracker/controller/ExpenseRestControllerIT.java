package com.mbeltran.budgettracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbeltran.budgettracker.dto.ExpenseRequest;
import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpenseRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anonymousRequestGetsJson401InsteadOfRedirect() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullCrudLifecycleForAnExpense() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        // Comida (categoryId=1 por ser la primera insertada en V2__seed_demo_data.sql) ya existe.
        ExpenseRequest createRequest = new ExpenseRequest(1L, new BigDecimal("42.00"), "Test lunch", LocalDate.now());

        String createResponse = mockMvc.perform(post("/api/expenses")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(42.00))
                .andExpect(jsonPath("$.description").value("Test lunch"))
                .andReturn().getResponse().getContentAsString();

        long createdId = objectMapper.readTree(createResponse).get("id").asLong();

        // Aparece en el listado
        mockMvc.perform(get("/api/expenses").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").exists());

        // Editar
        ExpenseRequest updateRequest = new ExpenseRequest(1L, new BigDecimal("99.00"), "Updated lunch", LocalDate.now());
        mockMvc.perform(put("/api/expenses/" + createdId)
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(99.00))
                .andExpect(jsonPath("$.description").value("Updated lunch"));

        // Borrar
        mockMvc.perform(delete("/api/expenses/" + createdId).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + createdId + ")]").doesNotExist());
    }

    @Test
    void rejectsExpenseWithNegativeAmount() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);
        ExpenseRequest invalid = new ExpenseRequest(1L, new BigDecimal("-5.00"), "invalido", LocalDate.now());

        mockMvc.perform(post("/api/expenses")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void returns404WhenDeletingExpenseThatDoesNotBelongToUser() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(delete("/api/expenses/999999").session(session).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void postWithoutCsrfTokenSucceedsBecauseApiChainHasCsrfDisabled() throws Exception {
        // /api/** ahora vive en su propia SecurityFilterChain (ver SecurityConfig), sin CSRF:
        // el cliente movil (JWT) nunca manda token CSRF, y el cliente jQuery (sesion) ya no
        // lo necesita. Antes de la introduccion del JWT esto devolvia 403; ese comportamiento
        // quedo intencionalmente obsoleto por el nuevo requisito de la API.
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);
        ExpenseRequest createRequest = new ExpenseRequest(1L, new BigDecimal("10.00"), "sin csrf", LocalDate.now());

        mockMvc.perform(post("/api/expenses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());
    }
}
