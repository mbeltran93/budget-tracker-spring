package com.mbeltran.budgettracker.controller;

import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresion explicita para Part A, requisito 9: las rutas server-rendered (/dashboard,
 * /categories) viven en la chain 2 (sesion + CSRF + formLogin) y no se enteran de que
 * ahora existe un JwtAuthFilter en la chain 1 ("/api/**"). Siguen exigiendo sesion,
 * siguen exigiendo CSRF en sus formularios, y un Bearer token (invalido o no) nunca
 * las autentica ni las rompe.
 */
class SessionRoutesUnaffectedByJwtIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardIgnoresAnyBearerTokenAndStillRedirectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-deberia-importar-aqui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void categoriesIgnoresAnyBearerTokenAndStillRedirectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-deberia-importar-aqui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void categoriesFormStillRequiresCsrfEvenWithASessionAndEvenWithABearerHeaderPresent() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(post("/categories")
                        .session(session)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-tampoco-deberia-importar-aqui")
                        .param("name", "Mascotas Sin Csrf")
                        .param("monthlyLimit", "10.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardWorksWithJustTheSessionExactlyAsBeforeJwtExisted() throws Exception {
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());
    }
}
