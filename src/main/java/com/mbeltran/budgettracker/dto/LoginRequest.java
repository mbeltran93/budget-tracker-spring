package com.mbeltran.budgettracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload JSON de POST /api/auth/login (cliente movil / cualquier consumidor de la API). */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
