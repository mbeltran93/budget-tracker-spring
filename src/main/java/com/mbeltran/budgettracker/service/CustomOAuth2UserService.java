package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.security.AppOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Login con Facebook (OAuth2 "plano", no OIDC: Facebook no expone un endpoint OIDC
 * estandar). Igual que CustomOidcUserService pero para el contrato OAuth2UserService.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserProvisioningService userProvisioningService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "email_not_available",
                    "El proveedor OAuth no devolvio un email (revisa el permiso 'email')",
                    null));
        }
        String name = (String) attributes.get("name");

        User user = userProvisioningService.findOrCreateFromOAuth("facebook", email, name);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
            nameAttributeKey = "id";
        }

        return new AppOAuth2User(user.getId(), oAuth2User.getAuthorities(), attributes, nameAttributeKey);
    }
}
