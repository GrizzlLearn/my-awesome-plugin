package com.noname.plugin.servlet;

import com.atlassian.jira.user.ApplicationUser;
import com.noname.plugin.servlet.handler.MailItemRequestHandler;
import com.noname.plugin.servlet.renderer.MailItemPageRenderer;
import com.noname.plugin.servlet.util.TestDataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * HTTP-точка входа для плагина просмотра почты.
 * Маршрутизирует GET-запросы к {@link MailItemPageRenderer} (HTML/CSS) и
 * к {@link MailItemRequestHandler} (JSON-данные), POST-запросы — к {@link MailItemRequestHandler}.
 * Не содержит бизнес-логики — только маршрутизация и проверка прав.
 *
 * Доступные маршруты:
 * <ul>
 *   <li>GET    {@code /mail-items/}       — таблица писем (HTML)</li>
 *   <li>GET    {@code /mail-items/data}   — все письма в виде JSON</li>
 *   <li>GET    {@code /mail-items/table}  — таблица писем (HTML, альтернативный путь)</li>
 *   <li>POST   {@code /delete-all}        — удалить все письма</li>
 *   <li>POST   {@code /create-test-data}  — создать тестовые данные</li>
 *   <li>POST   {@code /add-email}         — добавить письмо через JSON-тело запроса</li>
 *   <li>DELETE {@code /mail-items/{uuid}} — удалить одно письмо по идентификатору</li>
 * </ul>
 */
public class MailViewerServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MailViewerServlet.class);

    private final MailItemRequestHandler requestHandler;
    private final MailItemPageRenderer pageRenderer;
    private final TestDataInitializer testDataInitializer;
    private final AuthorizationService authorizationService;

    @Inject
    public MailViewerServlet(MailItemRequestHandler requestHandler,
                             MailItemPageRenderer pageRenderer,
                             TestDataInitializer testDataInitializer,
                             AuthorizationService authorizationService) {
        this.requestHandler = checkNotNull(requestHandler);
        this.pageRenderer = checkNotNull(pageRenderer);
        this.testDataInitializer = checkNotNull(testDataInitializer);
        this.authorizationService = checkNotNull(authorizationService);
    }

    /**
     * Маршрутизирует GET-запросы по URI.
     * Запрос на базовый URL без завершающего слеша перенаправляется на URL со слешем.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String requestURI = req.getRequestURI();

            if (requestURI.endsWith(MAIL_ITEMS_BASE)) {
                resp.sendRedirect(requestURI + "/");
                return;
            }

            if (requestURI.contains("/css/")) {
                pageRenderer.serveCssFile(req, resp);
                return;
            }

            if (!authorizationService.isSystemAdmin()) {
                handleUnauthorizedRequest(resp);
                return;
            }

            if (requestURI.endsWith(MAIL_ITEMS_ROOT)) {
                pageRenderer.renderTablePage(req, resp);
            } else if (requestURI.endsWith(MAIL_ITEMS_DATA)) {
                requestHandler.handleDataRequest(req, resp);
            } else if (requestURI.endsWith(MAIL_ITEMS_TABLE)) {
                pageRenderer.renderTablePage(req, resp);
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
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (!authorizationService.isSystemAdmin()) {
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
     * Маршрутизирует DELETE-запросы для удаления письма по UUID.
     * Путь: DELETE {@code /mail-items/{uuid}}.
     * Требует прав системного администратора JIRA.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (!authorizationService.isSystemAdmin()) {
                handleUnauthorizedRequest(resp);
                return;
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                String uuid = pathInfo.substring(1);
                requestHandler.handleDeleteByIdRequest(uuid, resp);
            } else {
                requestHandler.handleNotFoundRequest(resp);
            }

        } catch (Exception e) {
            log.error("Error handling DELETE request: " + req.getRequestURI(), e);
            requestHandler.handleInternalError(resp, e);
        }
    }

    /**
     * Обрабатывает запросы от неавторизованных пользователей.
     * Неаутентифицированных перенаправляет на страницу входа; аутентифицированным без прав возвращает 403.
     */
    private void handleUnauthorizedRequest(HttpServletResponse resp) throws IOException {
        ApplicationUser user = authorizationService.getLoggedInUser();
        if (user == null) {
            resp.sendRedirect(JIRA_LOGIN_URL);
        } else {
            requestHandler.handleForbiddenRequest(resp);
        }
    }
}
