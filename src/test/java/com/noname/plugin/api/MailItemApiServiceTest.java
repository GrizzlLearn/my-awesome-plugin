package com.noname.plugin.api;

import com.atlassian.jira.mail.Email;
import com.noname.plugin.model.MailItem;
import com.noname.plugin.service.MailItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailItemApiService — публичный API плагина")
class MailItemApiServiceTest {

    @Mock private MailItemService mailItemService;
    @Mock private MailItem mockItem;

    private MailItemApiService api;

    @BeforeEach
    void setUp() {
        api = new MailItemApiService(mailItemService);
    }

    // ===== addEmail(Email) =====

    @Test
    @DisplayName("addEmail(null) — бросает IllegalArgumentException без обращения к сервису")
    void addEmail_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> api.addEmail((Email) null));
        verifyNoInteractions(mailItemService);
    }

    @Test
    @DisplayName("addEmail(Email) — вызывает createMailItem и возвращает UUID")
    void addEmail_email_delegatesToServiceAndReturnsUuid() {
        Email email = mock(Email.class);
        when(mailItemService.createMailItem(email)).thenReturn("uuid-123");

        String result = api.addEmail(email);

        assertEquals("uuid-123", result);
        verify(mailItemService).createMailItem(email);
    }

    // ===== getEmailCount =====

    @Test
    @DisplayName("getEmailCount() — использует countMailItems(), а не getAllMailItems().size()")
    void getEmailCount_usesCountMailItems() {
        when(mailItemService.countMailItems()).thenReturn(7);

        assertEquals(7, api.getEmailCount());
        verify(mailItemService).countMailItems();
        verify(mailItemService, never()).getAllMailItems();
    }

    // ===== deleteAllEmails =====

    @Test
    @DisplayName("deleteAllEmails() — возвращает true, когда сервис удалил письма")
    void deleteAllEmails_withItems_returnsTrue() {
        when(mailItemService.deleteAllMailItemsSafe()).thenReturn(true);
        assertTrue(api.deleteAllEmails());
    }

    @Test
    @DisplayName("deleteAllEmails() — возвращает false, когда база была пуста")
    void deleteAllEmails_emptyDatabase_returnsFalse() {
        when(mailItemService.deleteAllMailItemsSafe()).thenReturn(false);
        assertFalse(api.deleteAllEmails());
    }

    // ===== loadTestData =====

    @Test
    @DisplayName("loadTestData() — вызывает сервис")
    void loadTestData_callsService() {
        api.loadTestData();
        verify(mailItemService).loadTestData();
    }

    // ===== getEmail* =====

    @Test
    @DisplayName("getEmailFrom() — возвращает поле from найденного письма")
    void getEmailFrom_returnsFromField() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getFrom()).thenReturn("sender@test.com");

        assertEquals("sender@test.com", api.getEmailFrom("id-1"));
    }

    @Test
    @DisplayName("getEmailTo() — возвращает поле to найденного письма")
    void getEmailTo_returnsToField() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getTo()).thenReturn("recipient@test.com");

        assertEquals("recipient@test.com", api.getEmailTo("id-1"));
    }

    @Test
    @DisplayName("getEmailSubject() — возвращает тему письма")
    void getEmailSubject_returnsSubject() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getSubject()).thenReturn("Тема письма");

        assertEquals("Тема письма", api.getEmailSubject("id-1"));
    }

    @Test
    @DisplayName("getEmailFrom() — несуществующий ID бросает IllegalArgumentException")
    void getEmailFrom_notFound_throwsIllegalArgument() {
        when(mailItemService.getMailItemById("bad-id")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> api.getEmailFrom("bad-id"));
        assertTrue(ex.getMessage().contains("Email not found: bad-id"));
    }

    @Test
    @DisplayName("getEmailCc() — возвращает поле cc найденного письма")
    void getEmailCc_returnsCcField() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getCc()).thenReturn("cc@test.com");

        assertEquals("cc@test.com", api.getEmailCc("id-1"));
    }

    @Test
    @DisplayName("getEmailBcc() — возвращает поле bcc найденного письма")
    void getEmailBcc_returnsBccField() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getBcc()).thenReturn("bcc@test.com");

        assertEquals("bcc@test.com", api.getEmailBcc("id-1"));
    }

    @Test
    @DisplayName("getEmailBodyHtml() — возвращает тело письма без изменений")
    void getEmailBodyHtml_returnsRawHtml() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getBody()).thenReturn("<p>Привет</p>");

        assertEquals("<p>Привет</p>", api.getEmailBodyHtml("id-1"));
    }

    @Test
    @DisplayName("getEmailBodyHtml() — не обрабатывает HTML через Jsoup (в отличие от getEmailBodyText)")
    void getEmailBodyHtml_doesNotStripTags() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        String html = "<h1>Заголовок</h1><p>Текст</p>";
        when(mockItem.getBody()).thenReturn(html);

        String result = api.getEmailBodyHtml("id-1");
        assertEquals(html, result);
        assertTrue(result.contains("<h1>"));
    }

    // ===== getEmailBodyText =====

    @Test
    @DisplayName("getEmailBodyText() — снимает HTML-теги через Jsoup")
    void getEmailBodyText_stripsHtmlTags() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getBody()).thenReturn("<h1>Заголовок</h1><p>Абзац с <strong>текстом</strong>.</p>");

        assertEquals("Заголовок Абзац с текстом.", api.getEmailBodyText("id-1"));
    }

    @Test
    @DisplayName("getEmailBodyText() — возвращает null, если тело письма не задано")
    void getEmailBodyText_nullBody_returnsNull() {
        when(mailItemService.getMailItemById("id-1")).thenReturn(mockItem);
        when(mockItem.getBody()).thenReturn(null);

        assertNull(api.getEmailBodyText("id-1"));
    }
}
