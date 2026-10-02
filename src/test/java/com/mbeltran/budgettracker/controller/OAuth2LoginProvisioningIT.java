package com.mbeltran.budgettracker.controller;

import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.repository.UserRepository;
import com.mbeltran.budgettracker.security.AppOAuth2User;
import com.mbeltran.budgettracker.security.AppOidcUser;
import com.mbeltran.budgettracker.service.UserProvisioningService;
import com.mbeltran.budgettracker.support.AbstractIntegrationTest;
import com.mbeltran.budgettracker.support.LoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part B, requisito 6: alta/vinculacion de usuarios desde un login social, probada contra
 * la base real (via el mismo UserProvisioningService que usan CustomOidcUserService y
 * CustomOAuth2UserService), mas el cableado del principal (AppOidcUser/AppOAuth2User ->
 * CurrentUser.id()) probado end-to-end contra un endpoint real detras de la sesion.
 *
 * No se simula el redirect completo /oauth2/authorization/google (necesitaria un proveedor
 * real); en cambio se construye el OidcUser/OAuth2User "a mano" con forma de Google/Facebook
 * y se inyecta con los request post-processors de spring-security-test, que es exactamente
 * el patron que sugiere la tarea cuando no hay credenciales reales disponibles.
 */
class OAuth2LoginProvisioningIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProvisioningService userProvisioningService;

    @Test
    void brandNewGoogleEmailCreatesExactlyOneNewUserRow() {
        String email = "nuevo." + UUID.randomUUID() + "@google-test.com";
        long countBefore = userRepository.count();

        User created = userProvisioningService.findOrCreateFromOAuth("google", email, "Primera Vez");

        assertThat(userRepository.count()).isEqualTo(countBefore + 1);
        assertThat(created.getId()).isNotNull();
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void loggingInAgainWithTheSameGoogleEmailDoesNotCreateASecondUserRow() {
        String email = "repetido." + UUID.randomUUID() + "@google-test.com";

        User firstLogin = userProvisioningService.findOrCreateFromOAuth("google", email, "Repetido");
        long countAfterFirstLogin = userRepository.count();

        User secondLogin = userProvisioningService.findOrCreateFromOAuth("google", email, "Repetido");

        assertThat(userRepository.count()).isEqualTo(countAfterFirstLogin);
        assertThat(secondLogin.getId()).isEqualTo(firstLogin.getId());
    }

    @Test
    void emailAlreadyRegisteredWithPasswordGetsLinkedInsteadOfDuplicated() {
        long countBefore = userRepository.count();
        User demoUser = userRepository.findByEmail(LoginSupport.DEMO_EMAIL).orElseThrow();

        // El usuario demo (demo@budgettracker.com) ya existe por registro email/password
        // (seed de Flyway). Si ahora "entra" con Facebook usando el mismo email, se tiene
        // que vincular a esa misma fila, no crear una cuenta duplicada.
        User linked = userProvisioningService.findOrCreateFromOAuth("facebook", LoginSupport.DEMO_EMAIL, "Demo Via Facebook");

        assertThat(userRepository.count()).isEqualTo(countBefore);
        assertThat(linked.getId()).isEqualTo(demoUser.getId());
    }

    @Test
    void requestAsOidcPrincipalResolvesCurrentUserIdAndCanHitCategoriesPage() throws Exception {
        String email = "categorias." + UUID.randomUUID() + "@google-test.com";
        AppOidcUser googleUser = googleShapedUser(email, "Ve Categorias");

        mockMvc.perform(get("/categories").with(oidcLogin().oidcUser(googleUser)))
                .andExpect(status().isOk());
    }

    @Test
    void requestAsPlainOAuth2PrincipalResolvesCurrentUserIdAndCanHitDashboard() throws Exception {
        String email = "facebook.dashboard." + UUID.randomUUID() + "@facebook-test.com";
        AppOAuth2User facebookUser = facebookShapedUser(email, "Ve Dashboard");

        mockMvc.perform(get("/dashboard").with(oauth2Login().oauth2User(facebookUser)))
                .andExpect(status().isOk());
    }

    /**
     * Construye un AppOidcUser real: primero provisiona/vincula el User en la base (igual
     * que haria CustomOidcUserService.loadUser) y recien ahi arma el principal con ese id,
     * para que GET /dashboard (detras de CurrentUser.id()) funcione de punta a punta.
     */
    private AppOidcUser googleShapedUser(String email, String name) {
        User user = userProvisioningService.findOrCreateFromOAuth("google", email, name);

        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("fake-id-token", now, now.plusSeconds(3600),
                Map.of("sub", "google-sub-" + user.getId(), "email", email));
        OidcUserInfo userInfo = new OidcUserInfo(Map.of(
                "sub", "google-sub-" + user.getId(), "email", email, "name", name));

        return new AppOidcUser(user.getId(), List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, userInfo);
    }

    private AppOAuth2User facebookShapedUser(String email, String name) {
        User user = userProvisioningService.findOrCreateFromOAuth("facebook", email, name);

        Map<String, Object> attributes = Map.of(
                "id", "facebook-id-" + user.getId(), "email", email, "name", name);
        return new AppOAuth2User(user.getId(), List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
    }
}
