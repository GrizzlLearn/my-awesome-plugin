package com.noname.plugin.servlet;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.noname.plugin.service.MailItemService;
import com.noname.plugin.servlet.handler.MailItemRequestHandler;
import com.noname.plugin.servlet.renderer.MailItemPageRenderer;
import com.noname.plugin.servlet.util.TestDataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Точка входа HTTP для просмотрщика писем плагина.
 * <p>
 * Маршрутизирует GET-запросы к {@link MailItemPageRenderer} (HTML/CSS) и
 * к {@link MailItemRequestHandler} (JSON-данные), а POST-запросы — к {@link MailItemRequestHandler}.
 * Сам сервлет не содержит бизнес-логики: только маршрутизация и проверка авторизации.
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>GET  {@code /mail-items/}       — таблица писем (HTML)</li>
 *   <li>GET  {@code /mail-items/data}   — все письма в JSON</li>
 *   <li>GET  {@code /mail-items/table}  — таблица писем (HTML, альтернативный путь)</li>
 *   <li>POST {@code /delete-all}        — удалить все письма</li>
 *   <li>POST {@code /create-test-data}  — создать тестовые данные</li>
 *   <li>POST {@code /add-email}         — добавить письмо через JSON</li>
 * </ul>
 */
public class MailViewerServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MailViewerServlet.class);

    private final MailItemRequestHandler requestHandler;
    private final MailItemPageRenderer pageRenderer;
    private final TestDataInitializer testDataInitializer;

    @Inject
    public MailViewerServlet(MailItemService mailItemService,
                             @ComponentImport PageBuilderService pageBuilderService) {
        MailItemService checkedMailItemService = checkNotNull(mailItemService);
        this.requestHandler = new MailItemRequestHandler(checkedMailItemService);
        this.pageRenderer = new MailItemPageRenderer(checkNotNull(pageBuilderService));
        this.testDataInitializer = new TestDataInitializer(checkedMailItemService);
    }

    /**
     * Маршрутизирует GET-запросы по URI.
     * Запрос на базовый URL без слеша перенаправляется на URL со слешем.
     *
     * @throws ServletException если возникла ошибка сервлета
     * @throws IOException      если возникла ошибка записи ответа
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String requestURI = req.getRequestURI();

            if (requestURI.endsWith(MAIL_ITEMS_BASE)) {
                resp.sendRedirect(requestURI + "/");
                return;
            }

            if (requestURI.endsWith(MAIL_ITEMS_ROOT)) {
                pageRenderer.renderTablePage(req, resp);
            } else if (requestURI.endsWith(MAIL_ITEMS_DATA)) {
                requestHandler.handleDataRequest(resp);
            } else if (requestURI.endsWith(MAIL_ITEMS_TABLE)) {
                pageRenderer.renderTablePage(req, resp);
            } else if (requestURI.contains("/css/")) {
                pageRenderer.serveCssFile(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found");
            }

        } catch (Exception e) {
            log.error("Error handling GET request: " + req.getRequestURI(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Маршрутизирует POST-запросы по pathInfo.
     * Все POST-операции требуют прав системного администратора JIRA.
     *
     * @throws IOException если возникла ошибка записи ответа
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (!isUserAuthorized()) {
                handleUnauthorizedRequest(resp);
                return;
            }

            String pathInfo = req.getPathInfo();

            if (DELETE_ALL_ENDPOINT.equals(pathInfo)) {
                requestHandler.handleDeleteAllRequest(resp);
            } else if (CREATE_TEST_DATA_ENDPOINT.equals(pathInfo)) {
                boolean created = testDataInitializer.forceCreateTestData();
                requestHandler.handleCreateTestDataRequest(resp, created);
            } else if (ADD_EMAIL_ENDPOINT.equals(pathInfo)) {
                requestHandler.handleAddEmailRequest(req, resp);
            } else {
                requestHandler.handleNotFoundRequest(resp);
            }

        } catch (Exception e) {
            log.error("Error handling POST request: " + req.getRequestURI(), e);
            requestHandler.handleInternalError(resp, e);
        }
    }

    /**
     * Проверяет, является ли текущий пользователь системным администратором JIRA.
     *
     * @return {@code true}, если пользователь вошёл в систему и имеет права системного администратора
     */
    private boolean isUserAuthorized() {
        UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        UserKey userKey = userManager.getRemoteUserKey();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        return user != null && userManager.isSystemAdmin(userKey);
    }

    /**
     * Обрабатывает запросы от неавторизованных пользователей.
     * Незалогиненных редиректит на страницу входа; залогиненных без прав — возвращает 403.
     *
     * @throws IOException если возникла ошибка записи ответа
     */
    private void handleUnauthorizedRequest(HttpServletResponse resp) throws IOException {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user == null) {
            resp.sendRedirect(JIRA_LOGIN_URL);
        } else {
            requestHandler.handleForbiddenRequest(resp);
        }
    }
}
