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
    @DisplayName("handleDataRequest: возвращает JSON со статусом 200")
    void handleDataRequest_returnsJsonWith200() throws Exception {
        when(mailItemService.getAllMailItemsAsJson()).thenReturn("[{\"id\":\"abc\"}]");

        handler.handleDataRequest(resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        verify(writer).write("[{\"id\":\"abc\"}]");
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
    @DisplayName("handleCreateTestDataRequest: при неудаче возвращает success=true с сообщением об ошибке")
    void handleCreateTestDataRequest_failed_returnsErrorMessage() throws IOException {
        handler.handleCreateTestDataRequest(resp, false);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertResponseContains("Failed to create test data");
    }

    // ===== handleAddEmailRequest =====

    @Test
    @DisplayName("handleAddEmailRequest: корректный JSON создаёт письмо и возвращает 201 с ID")
    void handleAddEmailRequest_validJson_returns201WithId() throws Exception {
        String json = "{\"from\":\"a@b.com\",\"to\":\"c@d.com\",\"subject\":\"Тема\",\"body\":\"Текст\"}";
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(json)));
        when(mailItemService.createMailItemFromJson(any())).thenReturn("test-uuid-123");

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);
        assertResponseContains("test-uuid-123");
    }

    @Test
    @DisplayName("handleAddEmailRequest: пустое тело запроса возвращает 400")
    void handleAddEmailRequest_emptyBody_returns400() throws IOException {
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertResponseContains("Request body is required");
    }

    @Test
    @DisplayName("handleAddEmailRequest: невалидный JSON возвращает 500")
    void handleAddEmailRequest_invalidJson_returns500() throws IOException {
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{not valid json")));

        handler.handleAddEmailRequest(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
    @DisplayName("handleInternalError: возвращает 500 с сообщением из исключения")
    void handleInternalError_returns500WithExceptionMessage() throws IOException {
        Exception ex = new RuntimeException("что-то сломалось");

        handler.handleInternalError(resp, ex);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertResponseContains("что-то сломалось");
    }

    @Test
    @DisplayName("handleInternalError: при null-сообщении в исключении возвращает дефолтный текст")
    void handleInternalError_nullExceptionMessage_returnsDefaultMessage() throws IOException {
        Exception ex = new RuntimeException((String) null);

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
