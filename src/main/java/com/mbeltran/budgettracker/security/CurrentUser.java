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
        throw new IllegalStateException("No hay un usuario autenticado en el contexto de seguridad");
    }
}
