package com.noname.plugin.servlet;

import com.noname.plugin.servlet.handler.MailItemRequestHandler;
import com.noname.plugin.servlet.renderer.MailItemPageRenderer;
import com.noname.plugin.servlet.util.TestDataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.noname.plugin.servlet.MailViewerConstants.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailViewerServlet — routing, auth gate, redirect logic")
class MailViewerServletTest {

    @Mock private MailItemRequestHandler requestHandler;
    @Mock private MailItemPageRenderer pageRenderer;
    @Mock private TestDataInitializer testDataInitializer;
    @Mock private AuthorizationService authorizationService;
    @Mock private HttpServletRequest req;
    @Mock private HttpServletResponse resp;

    private MailViewerServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new MailViewerServlet(requestHandler, pageRenderer, testDataInitializer, authorizationService);
    }

    // ===== doGet — redirect logic =====

    @Test
    @DisplayName("doGet: bare /mail-items URI redirects to /mail-items/")
    void doGet_bareMailItems_redirectsWithTrailingSlash() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items");

        servlet.doGet(req, resp);

        verify(resp).sendRedirect("/jira/plugins/servlet/mail-items/");
        verifyNoInteractions(authorizationService, pageRenderer, requestHandler);
    }

    // ===== doGet — CSS served without auth =====

    @Test
    @DisplayName("doGet: CSS path is served without authorization check")
    void doGet_cssPath_servedWithoutAuth() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/css/mail-main.css");

        servlet.doGet(req, resp);

        verify(pageRenderer).serveCssFile(req, resp);
        verifyNoInteractions(authorizationService);
    }

    // ===== doGet — auth gate =====

    @Test
    @DisplayName("doGet: unauthenticated user (null) on protected route redirects to login")
    void doGet_unauthenticatedUser_redirectsToLogin() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/");
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(null);

        servlet.doGet(req, resp);

        verify(resp).sendRedirect(JIRA_LOGIN_URL);
        verifyNoInteractions(pageRenderer, requestHandler);
    }

    @Test
    @DisplayName("doGet: authenticated non-admin on protected route returns 403")
    void doGet_authenticatedNonAdmin_returnsForbidden() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/");
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(mock(com.atlassian.jira.user.ApplicationUser.class));

        servlet.doGet(req, resp);

        verify(requestHandler).handleForbiddenRequest(resp);
        verifyNoInteractions(pageRenderer);
    }

    // ===== doGet — routing (authorized) =====

    @Test
    @DisplayName("doGet: /mail-items/ renders table page")
    void doGet_rootPath_rendersTablePage() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/");
        when(authorizationService.isSystemAdmin()).thenReturn(true);

        servlet.doGet(req, resp);

        verify(pageRenderer).renderTablePage(req, resp);
    }

    @Test
    @DisplayName("doGet: /mail-items/data delegates to requestHandler.handleDataRequest")
    void doGet_dataPath_delegatesToHandler() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/data");
        when(authorizationService.isSystemAdmin()).thenReturn(true);

        servlet.doGet(req, resp);

        verify(requestHandler).handleDataRequest(req, resp);
    }

    @Test
    @DisplayName("doGet: /mail-items/table renders table page")
    void doGet_tablePath_rendersTablePage() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/table");
        when(authorizationService.isSystemAdmin()).thenReturn(true);

        servlet.doGet(req, resp);

        verify(pageRenderer).renderTablePage(req, resp);
    }

    @Test
    @DisplayName("doGet: unknown path returns 404")
    void doGet_unknownPath_returns404() throws IOException {
        when(req.getRequestURI()).thenReturn("/jira/plugins/servlet/mail-items/unknown");
        when(authorizationService.isSystemAdmin()).thenReturn(true);

        servlet.doGet(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND, "Page not found");
    }

    // ===== doPost — auth gate =====

    @Test
    @DisplayName("doPost: unauthenticated user redirects to login")
    void doPost_unauthenticated_redirectsToLogin() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(null);

        servlet.doPost(req, resp);

        verify(resp).sendRedirect(JIRA_LOGIN_URL);
        verifyNoInteractions(requestHandler, testDataInitializer);
    }

    @Test
    @DisplayName("doPost: authenticated non-admin returns 403")
    void doPost_authenticatedNonAdmin_returnsForbidden() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(mock(com.atlassian.jira.user.ApplicationUser.class));

        servlet.doPost(req, resp);

        verify(requestHandler).handleForbiddenRequest(resp);
    }

    // ===== doPost — routing (authorized) =====

    @Test
    @DisplayName("doPost: /delete-all delegates to handleDeleteAllRequest")
    void doPost_deleteAll_delegatesToHandler() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn(DELETE_ALL_ENDPOINT);

        servlet.doPost(req, resp);

        verify(requestHandler).handleDeleteAllRequest(resp);
    }

    @Test
    @DisplayName("doPost: /create-test-data calls initializer then handler")
    void doPost_createTestData_callsInitializerAndHandler() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn(CREATE_TEST_DATA_ENDPOINT);
        when(testDataInitializer.forceCreateTestData()).thenReturn(true);

        servlet.doPost(req, resp);

        verify(testDataInitializer).forceCreateTestData();
        verify(requestHandler).handleCreateTestDataRequest(resp, true);
    }

    @Test
    @DisplayName("doPost: /add-email delegates to handleAddEmailRequest")
    void doPost_addEmail_delegatesToHandler() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn(ADD_EMAIL_ENDPOINT);

        servlet.doPost(req, resp);

        verify(requestHandler).handleAddEmailRequest(req, resp);
    }

    @Test
    @DisplayName("doPost: unknown path delegates to handleNotFoundRequest")
    void doPost_unknownPath_delegatesToNotFound() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn("/unknown-endpoint");

        servlet.doPost(req, resp);

        verify(requestHandler).handleNotFoundRequest(resp);
    }

    // ===== doDelete — auth gate =====

    @Test
    @DisplayName("doDelete: unauthenticated user redirects to login")
    void doDelete_unauthenticated_redirectsToLogin() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(null);

        servlet.doDelete(req, resp);

        verify(resp).sendRedirect(JIRA_LOGIN_URL);
        verifyNoInteractions(requestHandler);
    }

    @Test
    @DisplayName("doDelete: authenticated non-admin returns 403")
    void doDelete_authenticatedNonAdmin_returnsForbidden() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(false);
        when(authorizationService.getLoggedInUser()).thenReturn(mock(com.atlassian.jira.user.ApplicationUser.class));

        servlet.doDelete(req, resp);

        verify(requestHandler).handleForbiddenRequest(resp);
    }

    // ===== doDelete — routing (authorized) =====

    @Test
    @DisplayName("doDelete: UUID in pathInfo delegates to handleDeleteByIdRequest")
    void doDelete_withUuid_delegatesToHandler() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn("/some-uuid-1234");

        servlet.doDelete(req, resp);

        verify(requestHandler).handleDeleteByIdRequest("some-uuid-1234", resp);
    }

    @Test
    @DisplayName("doDelete: null pathInfo delegates to handleNotFoundRequest")
    void doDelete_nullPathInfo_delegatesToNotFound() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn(null);

        servlet.doDelete(req, resp);

        verify(requestHandler).handleNotFoundRequest(resp);
    }

    @Test
    @DisplayName("doDelete: pathInfo of just '/' delegates to handleNotFoundRequest")
    void doDelete_slashOnlyPathInfo_delegatesToNotFound() throws IOException {
        when(authorizationService.isSystemAdmin()).thenReturn(true);
        when(req.getPathInfo()).thenReturn("/");

        servlet.doDelete(req, resp);

        verify(requestHandler).handleNotFoundRequest(resp);
    }
}
