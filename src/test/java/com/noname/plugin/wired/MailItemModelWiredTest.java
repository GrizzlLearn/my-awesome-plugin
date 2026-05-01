package com.noname.plugin.wired;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.noname.plugin.api.MailItemApiService;
import com.noname.plugin.model.MailItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Wired-тесты для {@link MailItem} и {@link com.noname.plugin.mapper.MailItemMapper}.
 * Запускаются внутри JIRA, потому что {@link MailItem} наследует JIRA-класс {@code Email},
 * который обращается к {@code ComponentAccessor} даже в конструкторе.
 * Маппер тестируется через API: создаём письмо → сохраняем → читаем поля обратно.
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class MailItemModelWiredTest {

    private MailItemApiService api;

    @Before
    public void setUp() {
        api = ComponentAccessor.getOSGiComponentInstanceOfType(MailItemApiService.class);
        api.deleteAllEmails();
    }

    // ===== MailItem — модель =====

    @Test
    public void api_idAssignedAfterPersist() {
        MailItem item = new MailItem("to@test.com");
        String id = api.addEmail(item);

        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    @Test
    public void api_idIsUniquePerEmail() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ids.add(api.addEmail(new MailItem("to" + i + "@test.com")));
        }
        assertEquals(10, ids.size());
    }

    @Test
    public void mailItem_singleRecipientConstructor() {
        MailItem item = new MailItem("to@test.com");

        assertEquals("to@test.com", item.getTo());
        assertNull(item.getCc());
        assertNull(item.getBcc());
    }

    @Test
    public void mailItem_fullAddressConstructor() {
        MailItem item = new MailItem("to@test.com", "cc@test.com", "bcc@test.com");

        assertEquals("to@test.com",  item.getTo());
        assertEquals("cc@test.com",  item.getCc());
        assertEquals("bcc@test.com", item.getBcc());
    }

    @Test
    public void mailItem_settersWorkCorrectly() {
        MailItem item = new MailItem("to@test.com");
        item.setFrom("from@test.com");
        item.setSubject("Тема");
        item.setBody("<p>Текст</p>");
        item.setAttachmentsName("doc.pdf");
        item.setRawHeaders("X-Spam: false");

        assertEquals("from@test.com", item.getFrom());
        assertEquals("Тема",          item.getSubject());
        assertEquals("<p>Текст</p>",  item.getBody());
        assertEquals("doc.pdf",       item.getAttachmentsName());
        assertEquals("X-Spam: false", item.getRawHeaders());
    }

    // ===== MailItemMapper — через API round-trip =====

    @Test
    public void mapper_allFieldsSurviveRoundTrip() {
        MailItem original = new MailItem("to@test.com", "cc@test.com", "bcc@test.com");
        original.setFrom("from@test.com");
        original.setSubject("Тема письма");
        original.setBody("<h1>Привет</h1>");

        String id = api.addEmail(original);

        assertEquals("from@test.com",  api.getEmailFrom(id));
        assertEquals("to@test.com",    api.getEmailTo(id));
        assertEquals("cc@test.com",    api.getEmailCc(id));
        assertEquals("bcc@test.com",   api.getEmailBcc(id));
        assertEquals("Тема письма",    api.getEmailSubject(id));
        assertEquals("<h1>Привет</h1>", api.getEmailBodyHtml(id));
    }

    @Test
    public void mapper_nullBodyHandledWithoutException() {
        MailItem item = new MailItem("to@test.com");
        item.setFrom("from@test.com");
        item.setSubject("Без тела");

        String id = api.addEmail(item);

        assertNull(api.getEmailBodyHtml(id));
        assertNull(api.getEmailBodyText(id));
    }
}
