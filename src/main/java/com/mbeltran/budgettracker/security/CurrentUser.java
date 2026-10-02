package com.mbeltran.budgettracker.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Pequeno helper para leer el id del usuario autenticado desde cualquier capa. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getId();
        }
        // Login social (Google/Facebook, OIDC o OAuth2 plano): AppOidcUser/AppOAuth2User.
        if (principal instanceof LocalUserIdAware localUserIdAware) {
            return localUserIdAware.getLocalUserId();
        }
        throw new IllegalStateException("No hay un usuario autenticado en el contexto de seguridad");
    }
}
