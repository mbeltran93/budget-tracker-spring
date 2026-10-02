package com.mbeltran.budgettracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbeltran.budgettracker.dto.GoogleTokenExchangeRequest;
import com.mbeltran.budgettracker.dto.LoginRequest;
import com.mbeltran.budgettracker.service.GoogleIdTokenPayload;
import com.mbeltran.budgettracker.service.GoogleIdTokenVerificationService;
import com.mbeltran.budgettracker.service.JwtService;
import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Autenticacion para la API ("cliente movil"): login email/password -> JWT (Part A) e
 * intercambio de id_token de Google -> JWT (Part B). Cubre tambien como /api/expenses
 * y /api/categories conviven con JWT y con la sesion clasica en la misma SecurityFilterChain
 * (ver SecurityConfig).
 */
class AuthRestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private GoogleIdTokenVerificationService googleIdTokenVerificationService;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void successfulLoginReturnsAJwtToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(LoginSupport.DEMO_EMAIL);
        request.setPassword(LoginSupport.DEMO_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401Json() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(LoginSupport.DEMO_EMAIL);
        request.setPassword("not-the-right-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithUnknownEmailReturns401Json() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("no-existe@test.com");
        request.setPassword("cualquiera123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expensesWithValidJwtAndNoSessionReturns200() throws Exception {
        String token = loginAndGetToken(LoginSupport.DEMO_EMAIL, LoginSupport.DEMO_PASSWORD);

        mockMvc.perform(get("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void categoriesWithValidJwtReturns200WithSeededCategories() throws Exception {
        String token = loginAndGetToken(LoginSupport.DEMO_EMAIL, LoginSupport.DEMO_PASSWORD);

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Comida')]").exists());
    }

    @Test
    void expensesWithNoTokenAndNoSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expensesWithGarbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt-valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expensesWithExistingSessionAndNoJwtStillWorks() throws Exception {
        // Confirma que la chain /api/** (IF_REQUIRED, no STATELESS) sigue leyendo la sesion
        // existente del cliente jQuery: el requisito central de Part A.
        MockHttpSession session = LoginSupport.loginAsDemoUser(mockMvc);

        mockMvc.perform(get("/api/expenses").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void googleMobileExchangeReturnsJwtWhenIdTokenIsValid() throws Exception {
        when(googleIdTokenVerificationService.verify(any()))
                .thenReturn(Optional.of(new GoogleIdTokenPayload("movil.google@test.com", "Usuario Movil", "sub-123")));

        GoogleTokenExchangeRequest request = new GoogleTokenExchangeRequest();
        request.setIdToken("un-id-token-cualquiera");

        String response = mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();
        // El JWT emitido ya sirve para pegarle a la API, igual que el de /api/auth/login.
        mockMvc.perform(get("/api/expenses").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        assertThatTokenBelongsTo(token, "movil.google@test.com");
    }

    @Test
    void googleMobileExchangeReturns401WhenIdTokenIsInvalid() throws Exception {
        when(googleIdTokenVerificationService.verify(any())).thenReturn(Optional.empty());

        GoogleTokenExchangeRequest request = new GoogleTokenExchangeRequest();
        request.setIdToken("token-invalido-o-falsificado");

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private void assertThatTokenBelongsTo(String token, String expectedEmail) {
        jwtService.extractEmail(token).ifPresentOrElse(
                email -> org.assertj.core.api.Assertions.assertThat(email).isEqualTo(expectedEmail),
                () -> org.assertj.core.api.Assertions.fail("El token emitido no tiene un email valido"));
    }
}
