package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Logica de alta/vinculacion de usuarios que llega desde un login social (Google/Facebook),
 * compartida por el flujo web (CustomOidcUserService / CustomOAuth2UserService, via redirect)
 * y el flujo movil (POST /api/auth/oauth/google, intercambiando un id_token por un JWT propio).
 *
 * Si ya existe un User con ese email (sea porque se registro con email/password, o porque ya
 * entro antes con el otro proveedor), se reutiliza esa misma fila -el email es UNIQUE- en vez
 * de crear un duplicado. Si no existe, se crea una cuenta nueva con un password placeholder
 * (hasheado, aleatorio, nunca usable) porque User.password es NOT NULL/@NotBlank pero estas
 * cuentas jamas loguean con contrasena.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User findOrCreateFromOAuth(String provider, String providerEmail, String providerName) {
        return userRepository.findByEmail(providerEmail)
                .orElseGet(() -> {
                    log.info("Creando usuario nuevo via login social ({}): {}", provider, providerEmail);
                    User user = User.builder()
                            .name((providerName == null || providerName.isBlank()) ? providerEmail : providerName)
                            .email(providerEmail)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .build();
                    return userRepository.save(user);
                });
    }
}
