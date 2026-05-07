package com.noname.plugin.security;

import com.atlassian.jira.user.ApplicationUser;

/**
 * Определяет, авторизован ли текущий пользователь запроса для работы с просмотрщиком почты.
 * Вынесен из {@link com.noname.plugin.servlet.MailViewerServlet}, чтобы сделать логику авторизации инжектируемой и тестируемой.
 */
public interface AuthorizationService {

    /** Возвращает {@code true}, если текущий авторизованный пользователь является системным администратором JIRA. */
    boolean isSystemAdmin();

    /** Возвращает текущего авторизованного пользователя JIRA или {@code null}, если сессия неактивна. */
    ApplicationUser getLoggedInUser();
}
