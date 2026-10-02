package com.mbeltran.budgettracker.security;

import com.mbeltran.budgettracker.service.JwtService;
import com.mbeltran.budgettracker.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee "Authorization: Bearer &lt;jwt&gt;", lo valida y -si es valido- deja un
 * {@link org.springframework.security.core.Authentication} con un {@code AppUserPrincipal}
 * en el SecurityContext (cargado via el mismo UserService.loadUserByUsername que usa
 * el login por sesion), para que CurrentUser.id() funcione exactamente igual sea cual
 * sea el metodo de autenticacion.
 *
 * Si el token falta o es invalido, NO se lanza ninguna excepcion: simplemente se sigue
 * sin autenticar y es la propia cadena de Spring Security (authorizeHttpRequests +
 * authenticationEntryPoint de la chain /api/**) la que decide si eso termina en 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            jwtService.extractEmail(token).ifPresent(email -> {
                try {
                    UserDetails userDetails = userService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (UsernameNotFoundException ex) {
                    // Token con firma/expiracion validas pero de un usuario que ya no existe: se ignora.
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
