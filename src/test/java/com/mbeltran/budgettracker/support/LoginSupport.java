package com.mbeltran.budgettracker.support;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hace un login real (POST /login) contra el usuario demo sembrado por Flyway
 * y devuelve la sesion resultante, para que los tests de MVC/REST la reutilicen
 * tal como lo haria un navegador real tras loguearse.
 */
public final class LoginSupport {

    public static final String DEMO_EMAIL = "demo@budgettracker.com";
    public static final String DEMO_PASSWORD = "demo1234";

    private LoginSupport() {
    }

    public static MockHttpSession loginAsDemoUser(MockMvc mockMvc) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(DEMO_EMAIL).password(DEMO_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getRequest().getSession(false);
    }
}
