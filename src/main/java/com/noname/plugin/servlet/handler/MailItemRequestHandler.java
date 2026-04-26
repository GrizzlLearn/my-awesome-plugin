package com.noname.plugin.servlet.handler;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.noname.plugin.service.MailItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.noname.plugin.servlet.MailViewerConstants.*;

/**
 * Обработчик HTTP-запросов для операций с письмами.
 * Содержит по одному методу на каждую операцию — читает запрос, вызывает сервис, формирует JSON-ответ.
 * Маршрутизация (какой метод вызвать) находится в {@link com.noname.plugin.servlet.MailViewerServlet}.
 */
public class MailItemRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(MailItemRequestHandler.class);

    private final MailItemService mailItemService;

    /**
     * @param mailItemService сервис для работы с письмами
     */
    public MailItemRequestHandler(MailItemService mailItemService) {
        this.mailItemService = checkNotNull(mailItemService);
    }

    /**
     * Отдаёт все письма в виде JSON-массива.
     * Соответствует GET {@code /mail-items/data}.
     *
     * @param resp HTTP-ответ
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleDataRequest(HttpServletResponse resp) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE);

        try {
            String jsonData = mailItemService.getAllMailItemsAsJson();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(jsonData);
        } catch (JSONException e) {
            log.error("Error converting mail items to JSON", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Удаляет все письма из базы данных.
     * Соответствует POST {@code /delete-all}.
     *
     * @param resp HTTP-ответ
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleDeleteAllRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            boolean deleted = mailItemService.deleteAllMailItemsSafe();
            JSONObject payload = new JSONObject().put("result", deleted);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(ok(deleted ? DELETE_SUCCESS_MESSAGE : DELETE_NOTHING_MESSAGE, payload).toString());
        } catch (Exception e) {
            log.error("Error deleting all mail items", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Возвращает результат создания тестовых данных.
     * Соответствует POST {@code /create-test-data}. Сама генерация данных выполнена до вызова этого метода.
     *
     * @param resp    HTTP-ответ
     * @param created {@code true}, если данные были успешно созданы
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleCreateTestDataRequest(HttpServletResponse resp, boolean created) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            JSONObject payload = new JSONObject().put("result", created);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(ok(created ? TEST_DATA_SUCCESS_MESSAGE : TEST_DATA_ERROR_MESSAGE, payload).toString());
        } catch (Exception e) {
            log.error("Error creating test data", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Добавляет письмо, переданное в теле запроса в формате JSON.
     * Соответствует POST {@code /add-email}.
     * Ожидаемые поля JSON: {@code from}, {@code to}, {@code cc}, {@code bcc}, {@code subject}, {@code body}.
     * Возвращает UUID созданной записи в поле {@code id}.
     *
     * @param req  HTTP-запрос с JSON-телом
     * @param resp HTTP-ответ
     * @throws IOException если возникла ошибка чтения запроса или записи ответа
     */
    public void handleAddEmailRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);

        try {
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuffer.append(line);
                }
            }

            if (jsonBuffer.length() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(err("Request body is required").toString());
                return;
            }

            JSONObject json = new JSONObject(jsonBuffer.toString());
            String uuid = mailItemService.createMailItemFromJson(json);

            JSONObject payload = new JSONObject().put("id", uuid);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(ok("Email added successfully", payload).toString());
        } catch (JSONException e) {
            log.error("Error parsing JSON request", e);
            handleInternalError(resp, e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid email data", e);
            handleInternalError(resp, e);
        } catch (Exception e) {
            log.error("Error adding email", e);
            handleInternalError(resp, e);
        }
    }

    /**
     * Отвечает 404 для неизвестных POST-эндпоинтов.
     *
     * @param resp HTTP-ответ
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleNotFoundRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write(err(ENDPOINT_NOT_FOUND_MESSAGE).toString());
    }

    /**
     * Отвечает 403 для пользователей без прав администратора.
     *
     * @param resp HTTP-ответ
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleForbiddenRequest(HttpServletResponse resp) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.getWriter().write(err(ACCESS_DENIED_MESSAGE).toString());
    }

    /**
     * Отвечает 500 с сообщением из исключения.
     *
     * @param resp HTTP-ответ
     * @param e    исключение, ставшее причиной ошибки
     * @throws IOException если возникла ошибка записи ответа
     */
    public void handleInternalError(HttpServletResponse resp, Exception e) throws IOException {
        setJsonResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String errorMessage = e.getMessage() != null ? e.getMessage() : INTERNAL_ERROR_MESSAGE;
        resp.getWriter().write(err(errorMessage).toString());
    }

    // ===== Вспомогательные методы =====

    private void setJsonResponseHeaders(HttpServletResponse resp) {
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    private JSONObject ok(String message) {
        try {
            return new JSONObject().put("success", true).put("message", message);
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to build JSON response", e);
        }
    }

    private JSONObject ok(String message, JSONObject extra) {
        JSONObject o = ok(message);
        if (extra != null) {
            try {
                java.util.Iterator<String> keys = extra.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    o.put(key, extra.get(key));
                }
            } catch (JSONException e) {
                throw new IllegalStateException("Failed to merge JSON payload", e);
            }
        }
        return o;
    }

    private JSONObject err(String message) {
        try {
            return new JSONObject().put("success", false).put("error", message);
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to build JSON error response", e);
        }
    }
}
