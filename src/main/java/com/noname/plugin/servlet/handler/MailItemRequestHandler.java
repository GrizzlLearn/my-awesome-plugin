package com.noname.plugin.servlet.handler;

import com.atlassian.jira.util.json.JSONException;
import com.noname.plugin.service.MailItemService;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Handles HTTP requests for mail item operations.
 * Separates request handling logic from servlet routing.
 * @author dl
 * @date 11.08.2025 22:35
 */
public class MailItemRequestHandler {
    private static final Logger log = Logger.getLogger(MailItemRequestHandler.class.getName());

    private final MailItemService mailItemService;

    public MailItemRequestHandler(MailItemService mailItemService) {
        this.mailItemService = checkNotNull(mailItemService);
    }

    /**
     * Handles requests for JSON data endpoint
     */
    public void handleDataRequest(HttpServletResponse resp) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE);

        try {
            String jsonData = mailItemService.getAllMailItemsAsJson();
            resp.getWriter().write(jsonData);
            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (JSONException e) {
            log.error("Error converting mail items to JSON", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Handles delete all mail items request
     */
    public void handleDeleteAllRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            boolean deleted = mailItemService.deleteAllMailItemsSafe();

            String jsonResponse = createSuccessResponse(
                    deleted,
                    deleted ? DELETE_SUCCESS_MESSAGE : DELETE_NOTHING_MESSAGE
            );

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(jsonResponse);

        } catch (Exception e) {
            log.error("Error deleting all mail items", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Handles create test data request
     */
    public void handleCreateTestDataRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            boolean created = mailItemService.loadTestData();

            String jsonResponse = createSuccessResponse(
                    created,
                    created ? TEST_DATA_SUCCESS_MESSAGE : TEST_DATA_ERROR_MESSAGE
            );

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(jsonResponse);

        } catch (Exception e) {
            log.error("Error creating test data", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Handles requests to non-existent endpoints
     */
    public void handleNotFoundRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write(createErrorResponse(ENDPOINT_NOT_FOUND_MESSAGE));
    }

    /**
     * Handles forbidden access requests
     */
    public void handleForbiddenRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.getWriter().write(createErrorResponse(ACCESS_DENIED_MESSAGE));
    }

    /**
     * Handles internal server errors
     */
    public void handleInternalError(HttpServletResponse resp, Exception e) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        String errorMessage = e.getMessage() != null ? e.getMessage() : INTERNAL_ERROR_MESSAGE;
        resp.getWriter().write(createErrorResponse(errorMessage));
    }

    // Helper methods

    private void setJsonResponseHeaders(HttpServletResponse resp) {
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    private String createSuccessResponse(boolean result, String message) {
        return String.format(
                "{\"success\":true,\"result\":%s,\"message\":\"%s\"}",
                result,
                escapeJsonString(message)
        );
    }

    private String createErrorResponse(String errorMessage) {
        return String.format(
                "{\"success\":false,\"error\":\"%s\"}",
                escapeJsonString(errorMessage)
        );
    }

    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
