package com.mbeltran.budgettracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separado de SecurityConfig a proposito: SecurityConfig depende de UserService
 * (para construir el DaoAuthenticationProvider) y UserService depende de
 * PasswordEncoder. Si el bean de PasswordEncoder viviera dentro de SecurityConfig,
 * ambas clases se necesitarian mutuamente y Spring no puede resolver ese ciclo.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
