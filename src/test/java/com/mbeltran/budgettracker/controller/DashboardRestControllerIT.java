package com.mbeltran.budgettracker.controller;

import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRequestGetsJson401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summaryIncludesSeededCategoriesWithBudgetStatus() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(get("/api/dashboard/summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalThisMonth").exists())
                .andExpect(jsonPath("$.categories[?(@.categoryName == 'Comida')]").exists())
                .andExpect(jsonPath("$.categories[?(@.categoryName == 'Servicios')].status").value("OVER"));
    }
}
