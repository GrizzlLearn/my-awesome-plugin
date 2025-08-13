package com.noname.plugin.servlet.handler;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.noname.plugin.service.MailItemService;
import com.noname.plugin.model.MailItem;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Обрабатывает HTTP-запросы для операций с почтовыми элементами.
 * Отделяет логику обработки запросов от маршрутизации сервлета.
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
     * Обрабатывает запросы к конечной точке данных JSON
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
     * Обрабатывает запрос на удаление всех почтовых элементов
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
     * Обрабатывает запрос на создание тестовых данных
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
     * Обрабатывает запрос на добавление email объекта через API
     */
    public void handleAddEmailRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            // Читаем JSON из тела запроса
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuffer.append(line);
                }
            }

            if (jsonBuffer.length() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(createErrorResponse("Request body is required"));
                return;
            }

            // Парсим JSON
            JSONObject json = new JSONObject(jsonBuffer.toString());
            
            // Создаем MailItem
            MailItem createdItem = mailItemService.createMailItemFromJson(json);

            // Возвращаем успешный ответ с ID созданного объекта
            String jsonResponse = String.format(
                    "{\"success\":true,\"message\":\"Email added successfully\",\"id\":\"%s\"}",
                    escapeJsonString(createdItem.getId())
            );

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(jsonResponse);

        } catch (JSONException e) {
            log.error("Error parsing JSON request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(createErrorResponse("Invalid JSON format: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid email data", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding email", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Обрабатывает запросы к несуществующим конечным точкам
     */
    public void handleNotFoundRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write(createErrorResponse(ENDPOINT_NOT_FOUND_MESSAGE));
    }

    /**
     * Обрабатывает запросы с запрещенным доступом
     */
    public void handleForbiddenRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.getWriter().write(createErrorResponse(ACCESS_DENIED_MESSAGE));
    }

    /**
     * Обрабатывает внутренние ошибки сервера
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
