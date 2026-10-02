package com.mbeltran.budgettracker.service;

import com.mbeltran.budgettracker.entity.User;
import com.mbeltran.budgettracker.security.AppOidcUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Login con Google (OIDC): delega en el OidcUserService por defecto de Spring Security
 * para obtener el perfil verificado, y despues auto-provisiona/vincula el User local
 * para que el principal final cargue su id (ver AppOidcUser + CurrentUser).
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserProvisioningService userProvisioningService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        User user = userProvisioningService.findOrCreateFromOAuth("google", email, name);

        return new AppOidcUser(user.getId(), oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
