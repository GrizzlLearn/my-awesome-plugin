package com.noname.plugin.security;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Реализует {@link AuthorizationService} через инжектируемый {@link UserManager} из SAL.
 * Позволяет тестировать авторизацию без статических вызовов {@link ComponentAccessor}.
 */
@Component
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserManager userManager;

    @Inject
    public AuthorizationServiceImpl(@ComponentImport UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean isSystemAdmin() {
        if (userManager == null) return false;
        UserKey userKey = userManager.getRemoteUserKey();
        return userKey != null && userManager.isSystemAdmin(userKey);
    }

    @Override
    public ApplicationUser getLoggedInUser() {
        return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }
}
