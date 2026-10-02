package com.mbeltran.budgettracker.controller;

import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerPageRendersForAnonymousUser() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registersNewUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Nueva Persona")
                        .param("email", "nueva.persona@test.com")
                        .param("password", "secreto123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void rejectsRegistrationWithDuplicateEmail() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Otro")
                        .param("email", LoginSupport.DEMO_EMAIL)
                        .param("password", "secreto123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }

    @Test
    void rejectsRegistrationWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("name", "Otro")
                        .param("email", "no-es-un-email")
                        .param("password", "secreto123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }
}
