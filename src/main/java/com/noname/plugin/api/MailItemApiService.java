package com.noname.plugin.api;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mail.Email;
import com.noname.plugin.model.MailItem;
import com.noname.plugin.service.MailItemService;
import org.jsoup.Jsoup;

/**
 * Публичный API плагина для работы с письмами из внешних скриптов (ScriptRunner, Spock-тесты).
 * <p>
 * Типичный сценарий использования:
 * <pre>
 *   def api = new MailItemApiService()
 *
 *   def email = new Email("recipient@example.com")
 *   email.setFrom("sender@example.com")
 *   email.setSubject("Тема")
 *   email.setBody("&lt;p&gt;Текст&lt;/p&gt;")
 *
 *   def id = api.addEmail(email)         // сохранить и получить ID
 *   api.getEmailSubject(id) == "Тема"    // проверить отдельное поле
 *   api.getEmailBodyText(id) == "Текст"  // проверить текст без HTML
 * </pre>
 * <p>
 * Класс использует {@link ComponentAccessor} для получения {@link MailItemService} из OSGi-контейнера,
 * поэтому должен создаваться только внутри запущенного JIRA-экземпляра.
 */
public class MailItemApiService {

    private final MailItemService mailItemService;

    public MailItemApiService() {
        this.mailItemService = ComponentAccessor.getOSGiComponentInstanceOfType(MailItemService.class);
    }

    // ===== Добавление писем =====

    /**
     * Сохраняет письмо в плагине и возвращает его идентификатор.
     * ID нужен для последующей проверки полей через {@code getEmail*(id)}.
     *
     * @param email объект письма для сохранения
     * @return UUID созданной записи
     * @throws IllegalArgumentException если {@code email} равен {@code null}
     */
    public String addEmail(Email email) {
        if (email == null) {
            throw new IllegalArgumentException("Email object cannot be null");
        }
        return mailItemService.createMailItem(email);
    }

    /**
     * Сохраняет письмо с базовым набором полей (без cc и bcc).
     *
     * @param from    адрес отправителя
     * @param to      адрес получателя
     * @param subject тема письма
     * @param body    тело письма (HTML)
     * @return UUID созданной записи
     */
    public String addEmail(String from, String to, String subject, String body) {
        Email email = new Email(to);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        return addEmail(email);
    }

    /**
     * Сохраняет письмо с полным набором адресных полей.
     *
     * @param from    адрес отправителя
     * @param to      адрес получателя
     * @param cc      адреса получателей в копии
     * @param bcc     адреса получателей в скрытой копии
     * @param subject тема письма
     * @param body    тело письма (HTML)
     * @return UUID созданной записи
     */
    public String addEmail(String from, String to, String cc, String bcc, String subject, String body) {
        Email email = new Email(to, cc, bcc);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        return addEmail(email);
    }

    // ===== Управление коллекцией =====

    /**
     * Возвращает общее количество писем, хранящихся в плагине.
     *
     * @return количество записей в базе данных
     */
    public int getEmailCount() {
        return mailItemService.getAllMailItems().size();
    }

    /**
     * Удаляет все письма из базы данных плагина.
     *
     * @return {@code true}, если хотя бы одно письмо было удалено; {@code false}, если база была пуста
     */
    public boolean deleteAllEmails() {
        return mailItemService.deleteAllMailItemsSafe();
    }

    // ===== Чтение отдельных полей письма по ID =====

    /**
     * @param id UUID письма, полученный при вызове {@link #addEmail}
     * @return адрес отправителя
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailFrom(String id) {
        return getOrThrow(id).getFrom();
    }

    /**
     * @param id UUID письма
     * @return адрес основного получателя (поле «Кому»)
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailTo(String id) {
        return getOrThrow(id).getTo();
    }

    /**
     * @param id UUID письма
     * @return адреса получателей в копии (поле «CC»); {@code null}, если поле не задано
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailCc(String id) {
        return getOrThrow(id).getCc();
    }

    /**
     * @param id UUID письма
     * @return адреса получателей в скрытой копии (поле «BCC»); {@code null}, если поле не задано
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailBcc(String id) {
        return getOrThrow(id).getBcc();
    }

    /**
     * @param id UUID письма
     * @return тема письма
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailSubject(String id) {
        return getOrThrow(id).getSubject();
    }

    /**
     * Возвращает тело письма как есть, в виде HTML-разметки.
     *
     * @param id UUID письма
     * @return HTML-тело письма
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailBodyHtml(String id) {
        return getOrThrow(id).getBody();
    }

    /**
     * Возвращает тело письма в виде plain-текста без HTML-тегов.
     * Используется в тестах для проверки текстового содержимого без необходимости разбирать разметку вручную.
     * Парсинг выполняется через Jsoup, поэтому корректно обрабатываются HTML-сущности и вложенные теги.
     *
     * @param id UUID письма
     * @return текст письма без HTML; {@code null}, если тело письма не задано
     * @throws IllegalArgumentException если письмо с таким ID не найдено
     */
    public String getEmailBodyText(String id) {
        String html = getOrThrow(id).getBody();
        if (html == null) return null;
        return Jsoup.parse(html).text();
    }

    // ===== Вспомогательные методы =====

    /**
     * Загружает письмо по ID или бросает исключение, если оно не найдено.
     *
     * @param id UUID письма
     * @return {@link MailItem}
     * @throws IllegalArgumentException если письмо не найдено
     */
    /**
     * Создаёт 5 тестовых писем с HTML-содержимым.
     * Используется для быстрого наполнения базы при ручном тестировании.
     *
     * @return {@code true} при успехе
     */
    public boolean loadTestData() {
        return mailItemService.loadTestData();
    }

    private MailItem getOrThrow(String id) {
        MailItem item = mailItemService.getMailItemById(id);
        if (item == null) throw new IllegalArgumentException("Email not found: " + id);
        return item;
    }
}
