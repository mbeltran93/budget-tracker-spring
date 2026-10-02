package com.mbeltran.budgettracker.controller;

import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Flujo clasico de formulario con recarga de pagina (th:action), a diferencia del CRUD AJAX de gastos. */
class CategoryControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listPageRendersSeededCategories() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(get("/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/list"))
                .andExpect(content().string(containsString("Comida")));
    }

    @Test
    void createsCategoryThenRedirectsToList() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(post("/categories").session(session).with(csrf())
                        .param("name", "Mascotas")
                        .param("monthlyLimit", "80.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        mockMvc.perform(get("/categories").session(session))
                .andExpect(content().string(containsString("Mascotas")));
    }

    @Test
    void rejectsCreationWithBlankName() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(post("/categories").session(session).with(csrf())
                        .param("name", "")
                        .param("monthlyLimit", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    @Test
    void rejectsDuplicateCategoryName() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(post("/categories").session(session).with(csrf())
                        .param("name", "Comida")
                        .param("monthlyLimit", "123.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/form"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));
    }

    @Test
    void anonymousUserCannotAccessCategories() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
