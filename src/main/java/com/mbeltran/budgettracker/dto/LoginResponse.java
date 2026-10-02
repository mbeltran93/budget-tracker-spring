package com.mbeltran.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Respuesta JSON de un login exitoso (email/password u oauth/google): el JWT a usar como Bearer. */
@Getter
@AllArgsConstructor
public class LoginResponse {

    private final String token;
}
