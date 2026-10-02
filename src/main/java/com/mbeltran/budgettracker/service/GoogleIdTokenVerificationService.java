package com.mbeltran.budgettracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

/**
 * Verifica server-side el id_token de Google que manda el SDK google_sign_in del cliente
 * movil, usando la libreria oficial de Google (valida firma, issuer y audience contra el
 * GOOGLE_MOBILE_CLIENT_ID -el client id de la app Android/iOS, distinto del client-id web).
 *
 * Si GOOGLE_MOBILE_CLIENT_ID no esta configurado, el verifier queda sin construir y
 * verify() siempre devuelve Optional.empty() (el endpoint /api/auth/oauth/google
 * simplemente responde 401 en vez de romper el arranque de la app).
 */
@Slf4j
@Service
public class GoogleIdTokenVerificationService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerificationService(@Value("${app.oauth.google-mobile-client-id:}") String mobileClientId) {
        if (mobileClientId == null || mobileClientId.isBlank()) {
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(mobileClientId))
                    .build();
        }
    }

    public Optional<GoogleIdTokenPayload> verify(String idToken) {
        if (verifier == null) {
            log.warn("GOOGLE_MOBILE_CLIENT_ID no configurado: se rechaza el intercambio de id_token de Google");
            return Optional.empty();
        }
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                return Optional.empty();
            }
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            return Optional.of(new GoogleIdTokenPayload(email, name, payload.getSubject()));
        } catch (GeneralSecurityException | java.io.IOException ex) {
            log.warn("No se pudo verificar el id_token de Google: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
