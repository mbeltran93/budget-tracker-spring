package com.mbeltran.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload JSON de POST /api/auth/oauth/google: el id_token que entrega el SDK google_sign_in del movil. */
@Getter
@Setter
@NoArgsConstructor
public class GoogleTokenExchangeRequest {

    @NotBlank(message = "idToken es obligatorio")
    private String idToken;
}
