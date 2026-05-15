package com.noname.plugin.servlet.handler;

import com.noname.plugin.service.MailItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailItemRequestHandler — обработка HTTP-запросов")
class MailItemRequestHandlerTest {

    @Mock private MailItemService mailItemService;
    @Mock private HttpServletResponse resp;
    @Mock private HttpServletRequest req;
    @Mock private PrintWriter writer;

    private MailItemRequestHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        when(resp.getWriter()).thenReturn(writer);
        handler = new MailItemRequestHandler(mailItemService);
    }

    // ===== handleDataRequest =====

    @Test
    @DisplayName("handleDataRequest: без тегов возвращает все письма со статусом 200")
    void handleDataRequest_noTags_returnsAllWith200() throws Exception {
        String json = "{\"items\":[{\"id\":\"abc\"}],\"total\":1,\"offset\":0,\"limit\":10}";
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn(null);
        when(req.getParameter("limit")).thenReturn(null);
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 0)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        verify(writer).write(json);
    }

    @Test
    @DisplayName("handleDataRequest: передаёт теги и параметры пагинации в сервис")
    void handleDataRequest_withTagsAndPagination_passesParams() throws Exception {
        String json = "{\"items\":[],\"total\":0,\"offset\":10,\"limit\":5}";
        String[] tags = {"example", "lorem"};
        when(req.getParameterValues("tag")).thenReturn(tags);
        when(req.getParameter("offset")).thenReturn("10");
        when(req.getParameter("limit")).thenReturn("5");
        when(mailItemService.getAllMailItemsAsJson(tags, 10, 5, 0)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        verify(mailItemService).getAllMailItemsAsJson(tags, 10, 5, 0);
    }

    @Test
    @DisplayName("handleDataRequest: отрицательные и нечисловые offset/limit заменяются дефолтными значениями")
    void handleDataRequest_withInvalidParams_usesDefaults() throws Exception {
        String json = "{\"items\":[],\"total\":0,\"offset\":0,\"limit\":10}";
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn("-5");
        when(req.getParameter("limit")).thenReturn("abc");
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 0)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        verify(mailItemService).getAllMailItemsAsJson(null, 0, 10, 0);
    }

    @Test
    @DisplayName("handleDataRequest: sinceId=42 передаётся в сервис")
    void handleDataRequest_withSinceId_passesToService() throws Exception {
        String json = "{\"items\":[],\"total\":0,\"offset\":0,\"limit\":10,\"maxId\":42}";
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn(null);
        when(req.getParameter("limit")).thenReturn(null);
        when(req.getParameter("sinceId")).thenReturn("42");
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 42)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(mailItemService).getAllMailItemsAsJson(null, 0, 10, 42);
    }

    @Test
    @DisplayName("handleDataRequest: нечисловой sinceId заменяется на 0")
    void handleDataRequest_nonNumericSinceId_usesDefault() throws Exception {
        String json = "{\"items\":[],\"total\":0,\"offset\":0,\"limit\":10,\"maxId\":0}";
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn(null);
        when(req.getParameter("limit")).thenReturn(null);
        when(req.getParameter("sinceId")).thenReturn("abc");
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 0)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(mailItemService).getAllMailItemsAsJson(null, 0, 10, 0);
    }

    @Test
    @DisplayName("handleDataRequest: отрицательный sinceId заменяется на 0")
    void handleDataRequest_negativeSinceId_usesDefault() throws Exception {
        String json = "{\"items\":[],\"total\":0,\"offset\":0,\"limit\":10,\"maxId\":0}";
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn(null);
        when(req.getParameter("limit")).thenReturn(null);
        when(req.getParameter("sinceId")).thenReturn("-5");
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 0)).thenReturn(json);

        handler.handleDataRequest(req, resp);

        verify(mailItemService).getAllMailItemsAsJson(null, 0, 10, 0);
    }

    @Test
    @DisplayName("handleDataRequest: JSONException от сервиса возвращает 500")
    void handleDataRequest_serviceThrowsJSONException_returns500() throws Exception {
        when(req.getParameterValues("tag")).thenReturn(null);
        when(req.getParameter("offset")).thenReturn(null);
        when(req.getParameter("limit")).thenReturn(null);
        when(mailItemService.getAllMailItemsAsJson(null, 0, 10, 0))
                .thenThrow(new com.atlassian.jira.util.json.JSONException("json error"));

        handler.handleDataRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // ===== handleDeleteAllRequest =====

    @Test
    @DisplayName("handleDeleteAllRequest: при наличии писем возвращает success=true")
    void handleDeleteAllRequest_withItems_returnsSuccess() throws IOException {
        when(mailItemService.deleteAllMailItemsSafe()).thenReturn(true);

        handler.handleDeleteAllRequest(resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertResponseContains("\"success\":true");
    }

    @Test
    @DisplayName("handleDeleteAllRequest: при пустой базе возвращает success=true с сообщением 'nothing to delete'")
    void handleDeleteAllRequest_noItems_returnsSuccessWithNothingMessage() throws IOException {
        when(mailItemService.deleteAllMailItemsSafe()).thenReturn(false);

        handler.handleDeleteAllRequest(resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertResponseContains("No mail items to delete");
    }

    // ===== handleCreateTestDataRequest =====

    @Test
    @DisplayName("handleCreateTestDataRequest: при успешном создании возвращает success=true")
    void handleCreateTestDataRequest_created_returnsSuccess() throws IOException {
        handler.handleCreateTestDataRequest(resp, true);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertResponseContains("\"success\":true");
        assertResponseContains("Test data created successfully");
    }

    @Test
    @DisplayName("handleCreateTestDataRequest: при неудаче возвращает 500")
    void handleCreateTestDataRequest_failed_returnsErrorMessage() throws IOException {
        handler.handleCreateTestDataRequest(resp, false);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertResponseContains("\"success\":false");
        assertResponseContains("Failed to create test data");
    }

    // ===== handleAddEmailRequest =====

    @Test
    @DisplayName("handleAddEmailRequest: корректный JSON создаёт письмо и возвращает 201 с ID")
    void handleAddEmailRequest_validJson_returns201WithId() throws Exception {
        String json = "{\"from\":\"a@b.com\",\"to\":\"c@d.com\",\"subject\":\"Тема\",\"body\":\"Текст\"}";
        when(req.getContentType()).thenReturn("application/json");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(mailItemService.createMailItemFromJson(any())).thenReturn("test-uuid-123");

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);
        assertResponseContains("test-uuid-123");
    }

    @Test
    @DisplayName("handleAddEmailRequest: пустое тело запроса возвращает 400")
    void handleAddEmailRequest_emptyBody_returns400() throws IOException {
        when(req.getContentType()).thenReturn("application/json");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertResponseContains("Request body is required");
    }

    @Test
    @DisplayName("handleAddEmailRequest: невалидный JSON возвращает 500")
    void handleAddEmailRequest_invalidJson_returns500() throws IOException {
        when(req.getContentType()).thenReturn("application/json");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{not valid json")));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("handleAddEmailRequest: отсутствует получатель — возвращает 400")
    void handleAddEmailRequest_missingRecipient_returns400() throws Exception {
        String json = "{\"from\":\"a@b.com\",\"subject\":\"Тема\"}";
        when(req.getContentType()).thenReturn("application/json");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(mailItemService.createMailItemFromJson(any()))
                .thenThrow(new IllegalArgumentException("At least one recipient field (to, cc, bcc) must be provided"));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertResponseContains("At least one recipient");
    }

    @Test
    @DisplayName("handleAddEmailRequest: Content-Type не application/json возвращает 415 с success=false")
    void handleAddEmailRequest_wrongContentType_returns415() throws IOException {
        when(req.getContentType()).thenReturn("text/plain");

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        assertResponseContains("\"success\":false");
    }

    @Test
    @DisplayName("handleAddEmailRequest: Content-Length превышает 1 МБ — возвращает 413")
    void handleAddEmailRequest_contentLengthTooLarge_returns413() throws IOException {
        when(req.getContentType()).thenReturn("application/json");
        when(req.getContentLength()).thenReturn(1024 * 1024 + 1);

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        assertResponseContains("too large");
    }

    @Test
    @DisplayName("handleAddEmailRequest: chunked тело превышает 1 МБ — возвращает 413")
    void handleAddEmailRequest_chunkedBodyTooLarge_returns413() throws IOException {
        when(req.getContentType()).thenReturn("application/json");
        when(req.getContentLength()).thenReturn(-1);
        String bigBody = "x".repeat(1024 * 1024 + 1);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(bigBody)));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        assertResponseContains("too large");
    }

    // ===== handleDeleteByIdRequest =====

    @Test
    @DisplayName("handleDeleteByIdRequest: существующее письмо — возвращает 200")
    void handleDeleteByIdRequest_existing_returns200() throws IOException {
        when(mailItemService.deleteMailItemById("uuid-1")).thenReturn(true);

        handler.handleDeleteByIdRequest("uuid-1", resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertResponseContains("Mail item deleted successfully");
    }

    @Test
    @DisplayName("handleDeleteByIdRequest: несуществующее письмо — возвращает 404")
    void handleDeleteByIdRequest_notFound_returns404() throws IOException {
        when(mailItemService.deleteMailItemById("no-such-uuid")).thenReturn(false);

        handler.handleDeleteByIdRequest("no-such-uuid", resp);

        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertResponseContains("Mail item not found");
    }

    // ===== handleForbiddenRequest / handleNotFoundRequest / handleInternalError =====

    @Test
    @DisplayName("handleForbiddenRequest: возвращает 403 с сообщением об ошибке доступа")
    void handleForbiddenRequest_returns403() throws IOException {
        handler.handleForbiddenRequest(resp);

        verify(resp).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertResponseContains("Admin rights required");
    }

    @Test
    @DisplayName("handleNotFoundRequest: возвращает 404")
    void handleNotFoundRequest_returns404() throws IOException {
        handler.handleNotFoundRequest(resp);

        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertResponseContains("Endpoint not found");
    }

    @Test
    @DisplayName("handleInternalError: всегда возвращает 500 со стандартным текстом, не раскрывая деталей исключения")
    void handleInternalError_returns500WithGenericMessage() throws IOException {
        Exception ex = new RuntimeException("что-то сломалось");

        handler.handleInternalError(resp, ex);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertResponseContains("Internal server error");
    }

    // ===== helper =====

    private void assertResponseContains(String expected) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(captor.capture());
        assertTrue(captor.getValue().contains(expected),
                "Ответ должен содержать «" + expected + "», но был: " + captor.getValue());
    }
}
