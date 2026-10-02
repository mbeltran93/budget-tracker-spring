package com.mbeltran.budgettracker.config;

import com.mbeltran.budgettracker.security.JwtAuthFilter;
import com.mbeltran.budgettracker.service.CustomOAuth2UserService;
import com.mbeltran.budgettracker.service.CustomOidcUserService;
import com.mbeltran.budgettracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Dos SecurityFilterChain explicitas, por orden de evaluacion:
 *
 * <ul>
 *   <li><b>Chain 1 ({@code @Order(1)}, securityMatcher "/api/**")</b>: la API interna
 *       (/api/expenses, /api/dashboard, /api/categories) mas /api/auth/** (login JWT e
 *       intercambio de id_token de Google). Sin CSRF (el cliente movil nunca va a mandar
 *       un token CSRF, y el cliente jQuery ya viene mandando el header igual, pero ya no
 *       hace falta: ver nota de SessionCreationPolicy abajo). Sin formLogin ni OAuth2Login.
 *       Un JwtAuthFilter autentica por Bearer token cuando no hay sesion.</li>
 *   <li><b>Chain 2 ({@code @Order(2)}, matchea todo lo demas)</b>: la app web
 *       server-rendered de siempre -sesion + CSRF + formLogin- mas, ahora, OAuth2 login
 *       (Google/Facebook) si hay credenciales configuradas.</li>
 * </ul>
 *
 * El punto fino de la chain 1 es como conviven sesion+JWT en las mismas rutas /api/**:
 * {@code SessionCreationPolicy.IF_REQUIRED} (NO {@code STATELESS}) porque STATELESS le
 * pone un {@code NullSecurityContextRepository} a la chain, que ignora cualquier sesion
 * existente -rompiendo las llamadas $.ajax de jQuery que ya vienen autenticadas por
 * cookie-. Con IF_REQUIRED, el SecurityContextHolderFilter sigue leyendo la sesion si
 * hay una (cliente web), y si no hay ninguna, el JwtAuthFilter autentica por Bearer
 * (cliente movil). Verificado con un test de integracion real para cada camino
 * (ApiAuthIT: sesion sin JWT -> 200, JWT sin sesion -> 200, nada -> 401, token invalido -> 401).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOidcUserService customOidcUserService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;
    @Value("${app.oauth.google.client-secret:}")
    private String googleClientSecret;
    @Value("${app.oauth.facebook.client-id:}")
    private String facebookClientId;
    @Value("${app.oauth.facebook.client-secret:}")
    private String facebookClientSecret;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /** Chain 1: API interna (JSON), JWT o sesion, sin CSRF. Ver Javadoc de la clase. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Chain 2: el resto de la app, sesion + CSRF + formLogin + (si hay credenciales) OAuth2 login. */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/webjars/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .exceptionHandling(handling -> handling
                        // Ya no hace falta un DelegatingAuthenticationEntryPoint con caso especial para
                        // /api/**: esas rutas ahora las maneja por completo (y exclusivamente) la chain 1.
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                );

        List<ClientRegistration> registrations = oauthClientRegistrations();
        if (!registrations.isEmpty()) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .clientRegistrationRepository(new InMemoryClientRegistrationRepository(registrations))
                    .userInfoEndpoint(info -> info
                            .oidcUserService(customOidcUserService)
                            .userService(customOAuth2UserService)
                    )
                    .failureUrl("/login?error")
            );
        }

        return http.build();
    }

    /**
     * Solo registra los proveedores OAuth2 que tengan client-id/secret configurados (via
     * GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET/FACEBOOK_CLIENT_ID/FACEBOOK_CLIENT_SECRET). Sin
     * ninguno configurado, oauth2Login() ni se activa: el arranque de la app y los tests no
     * dependen de tener esas credenciales. Se construye a mano (no via las propiedades
     * estandar spring.security.oauth2.client.*) justamente para poder hacer este check: Spring
     * Boot exige client-id no vacio para cualquier registration declarada bajo esas propiedades,
     * y fallaria el arranque con las credenciales en blanco.
     */
    private List<ClientRegistration> oauthClientRegistrations() {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (isConfigured(googleClientId, googleClientSecret)) {
            registrations.add(googleClientRegistration());
        }
        if (isConfigured(facebookClientId, facebookClientSecret)) {
            registrations.add(facebookClientRegistration());
        }
        return registrations;
    }

    private boolean isConfigured(String clientId, String clientSecret) {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistrations.fromOidcIssuerLocation("https://accounts.google.com")
                .registrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .scope("openid", "profile", "email")
                .build();
    }

    /** Facebook no es OIDC: Spring Boot no lo trae preconfigurado, hay que declararlo a mano. */
    private ClientRegistration facebookClientRegistration() {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(facebookClientId)
                .clientSecret(facebookClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v19.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v19.0/oauth/token")
                .userInfoUri("https://graph.facebook.com/v19.0/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}
