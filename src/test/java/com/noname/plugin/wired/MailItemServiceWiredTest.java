package com.noname.plugin.wired;

import com.atlassian.jira.mail.Email;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.noname.plugin.api.MailItemApiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Wired-тесты для {@link com.noname.plugin.service.MailItemService} через публичный API.
 * Запускаются внутри реального JIRA-экземпляра с настоящим Active Objects.
 * Используют {@link MailItemApiService} — единственный экспортируемый OSGi-интерфейс плагина.
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class MailItemServiceWiredTest {

    private MailItemApiService api;

    @Before
    public void setUp() {
        api = new MailItemApiService();
        api.deleteAllEmails();
    }

    // ===== addEmail =====

    @Test
    public void addEmail_returnsNonEmptyId() {
        String id = api.addEmail("from@test.com", "to@test.com", "Тема", "<p>Текст</p>");

        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    @Test
    public void addEmail_nullEmail_throwsIllegalArgument() {
        try {
            api.addEmail(null);
            fail("Должно быть выброшено IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Email object cannot be null"));
        }
    }

    // ===== getEmail* =====

    @Test
    public void getEmailFields_returnCorrectValues() {
        Email email = new Email("to@test.com");
        email.setFrom("from@test.com");
        email.setSubject("Привет");
        email.setBody("<p>Текст письма</p>");
        String id = api.addEmail(email);

        assertEquals("from@test.com", api.getEmailFrom(id));
        assertEquals("to@test.com",   api.getEmailTo(id));
        assertEquals("Привет",        api.getEmailSubject(id));
        assertEquals("<p>Текст письма</p>", api.getEmailBodyHtml(id));
        assertEquals("Текст письма",  api.getEmailBodyText(id));
    }

    @Test
    public void getEmailFields_withCcAndBcc() {
        String id = api.addEmail("from@test.com", "to@test.com", "cc@test.com", "bcc@test.com",
                "Тема", "<p>Тело</p>");

        assertEquals("cc@test.com",  api.getEmailCc(id));
        assertEquals("bcc@test.com", api.getEmailBcc(id));
    }

    @Test
    public void getEmailById_nonExistentId_throwsIllegalArgument() {
        try {
            api.getEmailSubject("00000000-0000-0000-0000-000000000000");
            fail("Должно быть выброшено IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Email not found"));
        }
    }

    @Test
    public void getEmailBodyText_stripsHtmlTags() {
        String id = api.addEmail("f@t.com", "t@t.com",
                "Тема", "<h1>Заголовок</h1><p>Абзац с <strong>текстом</strong>.</p>");

        assertEquals("Заголовок Абзац с текстом.", api.getEmailBodyText(id));
    }

    // ===== getEmailCount =====

    @Test
    public void getEmailCount_emptyDatabase_returnsZero() {
        assertEquals(0, api.getEmailCount());
    }

    @Test
    public void getEmailCount_afterCreation_returnsCorrectCount() {
        api.addEmail("a@test.com", "x@test.com", "Письмо 1", "Тело");
        api.addEmail("b@test.com", "y@test.com", "Письмо 2", "Тело");
        api.addEmail("c@test.com", "z@test.com", "Письмо 3", "Тело");

        assertEquals(3, api.getEmailCount());
    }

    // ===== deleteAllEmails =====

    @Test
    public void deleteAllEmails_emptyDatabase_returnsFalse() {
        assertFalse(api.deleteAllEmails());
    }

    @Test
    public void deleteAllEmails_withItems_returnsTrueAndClearsDatabase() {
        api.addEmail("a@test.com", "x@test.com", "Тема", "Тело");
        api.addEmail("b@test.com", "y@test.com", "Тема", "Тело");

        assertTrue(api.deleteAllEmails());
        assertEquals(0, api.getEmailCount());
    }

    // ===== loadTestData =====

    @Test
    public void loadTestData_creates5Items() {
        api.loadTestData();

        assertEquals(5, api.getEmailCount());
    }

    @Test
    public void loadTestData_calledTwice_creates10Items() {
        api.loadTestData();
        api.loadTestData();

        assertEquals(10, api.getEmailCount());
    }
}
