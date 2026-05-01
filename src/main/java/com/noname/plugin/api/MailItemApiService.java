package com.noname.plugin.api;

import com.atlassian.jira.mail.Email;
import com.noname.plugin.model.MailItem;

/**
 * Публичный OSGi-контракт плагина для работы с письмами.
 * Используется из внешних скриптов (ScriptRunner, Spock-тесты) через {@code @PluginModule}.
 * <p>
 * Пример (Spock):
 * <pre>
 *   {@literal @}WithPlugin("com.noname.plugin.mail-catcher")
 *   class MySpec extends Specification {
 *
 *       {@literal @}PluginModule
 *       MailItemApiService api
 *
 *       def "test"() {
 *           def id = api.addEmail("from@test.com", "to@test.com", "Тема", "&lt;p&gt;Текст&lt;/p&gt;")
 *           api.getEmailSubject(id) == "Тема"
 *       }
 *   }
 * </pre>
 */
public interface MailItemApiService {

    String addEmail(Email email);

    String addEmail(String from, String to, String subject, String body);

    String addEmail(String from, String to, String cc, String bcc, String subject, String body);

    int getEmailCount();

    boolean deleteAllEmails();

    void loadTestData();

    String getEmailFrom(String id);

    String getEmailTo(String id);

    String getEmailCc(String id);

    String getEmailBcc(String id);

    String getEmailSubject(String id);

    String getEmailBodyHtml(String id);

    String getEmailBodyText(String id);
}
