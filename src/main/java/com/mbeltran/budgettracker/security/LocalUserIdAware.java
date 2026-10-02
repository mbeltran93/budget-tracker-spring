package com.mbeltran.budgettracker.security;

/**
 * Lo implementan los principales que no son {@link AppUserPrincipal} (es decir, los que
 * vienen de un login social OAuth2/OIDC) pero que igual envuelven el id de un {@code User}
 * local, para que {@link CurrentUser#id()} los pueda resolver igual que a cualquier otro.
 */
public interface LocalUserIdAware {

    Long getLocalUserId();
}
