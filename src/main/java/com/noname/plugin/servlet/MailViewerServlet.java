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
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Основной сервлет для функциональности просмотра почтовых элементов.
 * Делегирует обязанности специализированным обработчикам и рендерам.
 *
 * @author dl
 * @date 24.06.2025 22:54
 */
public class MailViewerServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(MailViewerServlet.class.getName());

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Инициализация тестовых данных если необходимо
            testDataInitializer.initializeIfEmpty();

            String requestURI = req.getRequestURI();

            // Redirect to proper URL format
            if (requestURI.endsWith(MAIL_ITEMS_BASE)) {
                resp.sendRedirect(requestURI + "/");
                return;
            }

            // Route requests to appropriate handlers
            if (requestURI.endsWith(MAIL_ITEMS_ROOT) || requestURI.endsWith(MAIL_ITEMS_BASE)) {
                pageRenderer.renderMainPage(resp);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Check authentication and authorization
            if (!isUserAuthorized()) {
                handleUnauthorizedRequest(resp);
                return;
            }

            String pathInfo = req.getPathInfo();

            if (DELETE_ALL_ENDPOINT.equals(pathInfo)) {
                requestHandler.handleDeleteAllRequest(resp);
            } else if (CREATE_TEST_DATA_ENDPOINT.equals(pathInfo)) {
                requestHandler.handleCreateTestDataRequest(resp);
            } else {
                requestHandler.handleNotFoundRequest(resp);
            }

        } catch (Exception e) {
            log.error("Error handling POST request: " + req.getRequestURI(), e);
            requestHandler.handleInternalError(resp, e);
        }
    }

    private boolean isUserAuthorized() {
        UserManager userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        UserKey userKey = userManager.getRemoteUserKey();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        return user != null && userManager.isSystemAdmin(userKey);
    }

    private void handleUnauthorizedRequest(HttpServletResponse resp) throws IOException {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        if (user == null) {
            resp.sendRedirect(JIRA_LOGIN_URL);
        } else {
            requestHandler.handleForbiddenRequest(resp);
        }
    }
}
