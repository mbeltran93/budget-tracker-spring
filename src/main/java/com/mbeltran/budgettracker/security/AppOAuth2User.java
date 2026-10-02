package com.mbeltran.budgettracker.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Principal OAuth2 "plano" (login con Facebook, que no es OIDC) que ademas carga el id
 * del {@code User} local ya creado/vinculado por {@code UserProvisioningService}, para que
 * {@link CurrentUser#id()} funcione exactamente igual que con un login por sesion clasico.
 */
public class AppOAuth2User extends DefaultOAuth2User implements LocalUserIdAware {

    private final Long localUserId;

    public AppOAuth2User(Long localUserId, Collection<? extends GrantedAuthority> authorities,
                          Map<String, Object> attributes, String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        this.localUserId = localUserId;
    }

    @Override
    public Long getLocalUserId() {
        return localUserId;
    }
}
