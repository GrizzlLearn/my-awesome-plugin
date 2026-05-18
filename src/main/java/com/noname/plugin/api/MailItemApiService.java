package com.noname.plugin.api;

import com.atlassian.jira.mail.Email;

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

    /** Сохраняет письмо и возвращает UUID созданной записи. */
    String addEmail(Email email);

    /** Сохраняет письмо и возвращает UUID созданной записи. */
    String addEmail(String from, String to, String subject, String body);

    /** Сохраняет письмо и возвращает UUID созданной записи. */
    String addEmail(String from, String to, String cc, String bcc, String subject, String body);

    /** Возвращает общее количество сохранённых писем. */
    int getEmailCount();

    /** Удаляет все письма; возвращает {@code true}, если хотя бы одна запись была удалена. */
    boolean deleteAllEmails();

    /** Удаляет письмо по UUID; возвращает {@code true}, если запись найдена и удалена. */
    boolean deleteEmailById(String id);

    /** Создаёт набор тестовых писем в базе данных. */
    void loadTestData();

    /** Возвращает адрес отправителя письма с указанным UUID. */
    String getEmailFrom(String id);

    /** Возвращает адрес получателя (To) письма с указанным UUID. */
    String getEmailTo(String id);

    /** Возвращает адрес получателя (Cc) письма с указанным UUID. */
    String getEmailCc(String id);

    /** Возвращает адрес получателя (Bcc) письма с указанным UUID. */
    String getEmailBcc(String id);

    /** Возвращает тему письма с указанным UUID. */
    String getEmailSubject(String id);

    /**
     * Возвращает HTML-тело письма в исходном виде (raw), без каких-либо изменений.
     * Тело хранится и возвращается точно в том виде, в каком было получено.
     */
    String getEmailBodyHtml(String id);

    /**
     * Возвращает тело письма в виде plain text.
     * HTML-разметка удаляется через Jsoup; результат содержит только текстовое содержимое.
     */
    String getEmailBodyText(String id);
}
