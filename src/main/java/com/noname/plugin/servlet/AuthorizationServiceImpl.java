package com.noname.plugin.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.stereotype.Component;

/**
 * Оборачивает статические вызовы {@link ComponentAccessor}, необходимые для авторизации,
 * чтобы {@link MailViewerServlet} можно было тестировать с мок-объектом {@link AuthorizationService}.
 */
@Component
public class AuthorizationServiceImpl implements AuthorizationService {

    @Override
    public boolean isSystemAdmin() {
        UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        if (userManager == null) return false;
        UserKey userKey = userManager.getRemoteUserKey();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        return user != null && userManager.isSystemAdmin(userKey);
    }

    @Override
    public ApplicationUser getLoggedInUser() {
        return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }
}
