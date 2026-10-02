package com.mbeltran.budgettracker.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

/**
 * Principal OIDC (login con Google) que ademas carga el id del {@code User} local
 * ya creado/vinculado por {@code UserProvisioningService}, para que {@link CurrentUser#id()}
 * funcione exactamente igual que con un login por sesion clasico.
 */
public class AppOidcUser extends DefaultOidcUser implements LocalUserIdAware {

    private final Long localUserId;

    public AppOidcUser(Long localUserId, Collection<? extends GrantedAuthority> authorities,
                        OidcIdToken idToken, OidcUserInfo userInfo) {
        super(authorities, idToken, userInfo);
        this.localUserId = localUserId;
    }

    @Override
    public Long getLocalUserId() {
        return localUserId;
    }
}
