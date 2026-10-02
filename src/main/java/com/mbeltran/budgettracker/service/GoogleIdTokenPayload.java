package com.mbeltran.budgettracker.service;

/** Datos ya verificados del id_token de Google que manda el cliente movil. */
public record GoogleIdTokenPayload(String email, String name, String subject) {
}
