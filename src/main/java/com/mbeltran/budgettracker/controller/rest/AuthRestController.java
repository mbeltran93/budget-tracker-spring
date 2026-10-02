package com.mbeltran.budgettracker.controller.rest;

import com.mbeltran.budgettracker.dto.GoogleTokenExchangeRequest;
import com.mbeltran.budgettracker.dto.LoginRequest;
import com.mbeltran.budgettracker.dto.LoginResponse;
import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.security.AppUserPrincipal;
import com.mbeltran.budgettracker.service.GoogleIdTokenPayload;
import com.mbeltran.budgettracker.service.GoogleIdTokenVerificationService;
import com.mbeltran.budgettracker.service.JwtService;
import com.mbeltran.budgettracker.service.UserProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticacion para clientes de la API (pensado para una futura app movil): no usa
 * sesion/cookie, emite un JWT. Vive bajo /api/auth/** que la chain 1 de SecurityConfig
 * deja publica a proposito (si no, nadie podria loguearse para conseguir el primer token).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleIdTokenVerificationService googleIdTokenVerificationService;
    private final UserProvisioningService userProvisioningService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // Reutiliza el mismo AuthenticationManager/DaoAuthenticationProvider (UserService +
        // PasswordEncoder) que usa formLogin: la validacion de credenciales no se reinventa.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getEmail(), principal.getId());
        return new LoginResponse(token);
    }

    @PostMapping("/oauth/google")
    public LoginResponse googleExchange(@Valid @RequestBody GoogleTokenExchangeRequest request) {
        GoogleIdTokenPayload payload = googleIdTokenVerificationService.verify(request.getIdToken())
                .orElseThrow(() -> new BadCredentialsException("El id_token de Google no es valido"));

        User user = userProvisioningService.findOrCreateFromOAuth("google", payload.email(), payload.name());
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return new LoginResponse(token);
    }
}
